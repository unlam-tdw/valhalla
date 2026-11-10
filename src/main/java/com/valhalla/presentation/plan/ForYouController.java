package com.valhalla.presentation.plan;

import com.valhalla.domain.bookmark.PlanBookmarkService;
import com.valhalla.domain.follow.FollowService;
import com.valhalla.domain.place.PlaceCategory;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.plan.PlanDiscovery;
import com.valhalla.domain.plan.PlanDiscoveryQuery;
import com.valhalla.domain.plan.PlanService;
import com.valhalla.domain.rating.PlanRatingService;
import com.valhalla.domain.rating.RatingSummary;
import com.valhalla.domain.user.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * [PPV] Feed "Para vos" en {@code GET /for-you}: planes publicos para descubrir, con o sin sesion.
 *
 * <p>Esta ruta vive fuera de {@code PlanController} porque ese controller esta amarrado a
 * {@code @RequestMapping("/plans")} y el feed ya no es una subruta de /plans. Sin sesion se muestran
 * todos los planes publicos (el filtro de propios no aplica: el anonimo no tiene planes propios);
 * con sesion los propios quedan fuera, porque el feed es para descubrir planes de otros.
 *
 * <p>[FOLLOW] El scope {@code following} es el mismo feed restringido a los autores que el usuario
 * sigue (AC-07, AC-08): mismo conjunto publico, mismos filtros y mismo orden, y solo con sesion
 * (AC-10). Los dos estados vacios propios viajan al modelo como {@code followingScope} y
 * {@code followsNobody} (AC-09).
 *
 * <p>[PAGE] El feed se sirve de a paginas despues de filtrar y ordenar, para que la pagina 2
 * respete el ranking de la 1. El endpoint parcial {@code /for-you?partial} devuelve el mismo
 * fragmento de cards que usa la pagina completa: "Cargar mas" lo pide y lo agrega sin recargar.
 */
@Controller
public class ForYouController {

  private static final String VIEW_PUBLIC_LIST = "pages/plans/public-list";
  private static final String VIEW_FEED_FRAGMENT = "components/feed-cards :: feedPage";
  private static final String ATTR_PUBLIC_PLANS = "publicPlans";
  private static final String SCOPE_FOLLOWING = "following";
  private static final String FEED_PATH = "/for-you";
  private static final String REDIRECT_LOGIN_FOLLOWING =
    "redirect:/auth/login?redirect=/for-you%3Fscope%3Dfollowing";

  private final PlanService planService;
  private final PlanRatingService planRatingService;
  private final PlanBookmarkService planBookmarkService;
  private final FollowService followService;
  private final UserService userService;

  @Autowired
  public ForYouController(
    PlanService planService,
    PlanRatingService planRatingService,
    PlanBookmarkService planBookmarkService,
    FollowService followService,
    UserService userService
  ) {
    this.planService = planService;
    this.planRatingService = planRatingService;
    this.planBookmarkService = planBookmarkService;
    this.followService = followService;
    this.userService = userService;
  }

  /**
   * [SEARCH] Feed filtrado y ordenado: {@code q}, {@code category}, {@code when} y {@code sort}
   * viajan en el query string y se parsean de forma tolerante, asi un parametro invalido nunca
   * rompe la pagina. Los valores aplicados vuelven en el modelo para que el formulario los refleje.
   *
   * <p>[FOLLOW] {@code scope=following} (sin distinguir mayusculas) elige los planes publicos de
   * los autores seguidos; cualquier otro valor cae al feed completo. Sin sesion, el scope
   * Siguiendo rebota al login con vuelta a la misma URL (AC-10).
   *
   * <p>[PAGE] {@code page} y {@code size} tienen default 1/12 y parseo tolerante. La paginacion
   * conserva los filtros en la URL: el link sin JS y el endpoint parcial llevan los mismos valores.
   */
  @GetMapping(FEED_PATH)
  public ModelAndView forYou(
    Authentication authentication,
    @RequestParam(name = "q", required = false) String queryText,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String when,
    @RequestParam(required = false) String sort,
    @RequestParam(required = false) String scope,
    @RequestParam(required = false) String page,
    @RequestParam(required = false) String size
  ) {
    return feed(
      authentication,
      queryText,
      category,
      when,
      sort,
      scope,
      page,
      size,
      VIEW_PUBLIC_LIST
    );
  }

  /**
   * [PAGE] La misma pagina sin layout, para "Cargar mas": devuelve solo las cards de la ventana
   * pedida y los datos de la siguiente. El script lo agrega al grid existente.
   */
  @GetMapping(value = FEED_PATH, params = "partial")
  public ModelAndView forYouPartial(
    Authentication authentication,
    @RequestParam(name = "q", required = false) String queryText,
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String when,
    @RequestParam(required = false) String sort,
    @RequestParam(required = false) String scope,
    @RequestParam(required = false) String page,
    @RequestParam(required = false) String size
  ) {
    return feed(
      authentication,
      queryText,
      category,
      when,
      sort,
      scope,
      page,
      size,
      VIEW_FEED_FRAGMENT
    );
  }

  /** Compatibilidad con los llamadores previos a [PAGE]: pagina 1 y doce por pagina. */
  ModelAndView forYou(
    Authentication authentication,
    String queryText,
    String category,
    String when,
    String sort,
    String scope
  ) {
    return feed(
      authentication,
      queryText,
      category,
      when,
      sort,
      scope,
      null,
      null,
      VIEW_PUBLIC_LIST
    );
  }

  private ModelAndView feed(
    Authentication authentication,
    String queryText,
    String category,
    String when,
    String sort,
    String scope,
    String page,
    String size,
    String viewName
  ) {
    boolean followingScope = isFollowingScope(scope);
    UUID viewerId = followingScope ? viewerId(authentication) : null;
    if (followingScope && viewerId == null) {
      return new ModelAndView(REDIRECT_LOGIN_FOLLOWING);
    }
    // Con anonymousAuthenticationFilter encendido el principal es un AnonymousAuthenticationToken
    // (nombre "anonymousUser"), no null: ningun plan tiene ese administrador, asi que el filtro de
    // propios deja pasar todo igual. El corte por null cubre el caso sin anonymous habilitado.
    String currentEmail = currentEmail(authentication);
    PlanDiscoveryQuery query = PlanDiscoveryQuery.parse(
      queryText,
      category,
      when,
      sort,
      page,
      size
    );
    List<UUID> followedIds = followingScope ? followService.followedIds(viewerId) : List.of();
    List<Plan> source = followingScope
      ? planService.getPublicPlansByAdministrators(followedIds)
      : planService.getPublicPlans();
    List<Plan> candidates = source.stream().filter(plan -> !ownedBy(plan, currentEmail)).toList();

    // [TOP] El orden popular necesita los agregados de TODOS los candidatos: recortar antes de
    // ordenar elegiria la pagina por otro criterio. Para fecha/nombre no hacen falta y las cards
    // piden solo los de su ventana [PAGE-008].
    RankingStats ranking = rankingStats(query, candidates);
    List<Plan> discovered = PlanDiscovery
      .discover(
        candidates,
        ranking.summaries(),
        ranking.participantCounts(),
        query,
        LocalDate.now()
      )
      .stream()
      .filter(plan -> !plan.isBanned(currentEmail))
      .toList();

    // [PAGE] Se recorta DESPUES del ranking y de excluir baneados: total/hasMore describen el
    // feed visible, no la lista intermedia. Una pagina fuera de rango cae en la ultima, para que
    // F5 y el fragmento parcial pinten lo mismo.
    int total = discovered.size();
    int lastPage = lastPage(total, query.size());
    int currentPage = Math.min(query.page(), lastPage);
    List<Plan> pagePlans = pageOf(discovered, currentPage, query.size());
    List<UUID> pagePlanIds = planIds(pagePlans);

    // [CAL] Summaries de la pagina que se pinta. En popular ya se pidieron para rankear y se
    // reutilizan; en fecha/nombre se piden recien para las cards de esta ventana.
    Map<UUID, RatingSummary> summaries = cardSummaries(query, ranking.summaries(), pagePlanIds);

    // [MARK] Un solo lookup alimenta el icono de todas las cards. Sin sesion no hay nada que
    // preguntar y el toggle no se renderiza (AC-05): no es un soft gate, es ausencia de control.
    boolean signedIn = signedIn(authentication);
    Set<UUID> bookmarkedPlanIds = bookmarkedPlanIds(authentication);
    Map<String, Object> model = new ModelMap();
    model.put(ATTR_PUBLIC_PLANS, feedCards(pagePlans, summaries, bookmarkedPlanIds));
    model.put("canBookmark", signedIn);
    model.put("q", query.text());
    model.put("selectedCategory", selectedCategory(query));
    model.put("selectedWhen", query.when().name().toLowerCase(Locale.ROOT));
    model.put("selectedSort", query.sort().name().toLowerCase(Locale.ROOT));
    model.put("filtersApplied", query.hasFilters());
    model.put("categories", PlaceCategory.values());
    // [PAGE] Datos de la ventana y URLs que conservan todos los filtros aplicados.
    model.put("page", currentPage);
    model.put("size", query.size());
    model.put("total", total);
    model.put("hasMore", currentPage < lastPage);
    model.put("feedCurrentUrl", feedUrl(currentPage, query, followingScope, false));
    model.put("nextPageUrl", feedUrl(currentPage + 1, query, followingScope, false));
    model.put("nextPartialUrl", feedUrl(currentPage + 1, query, followingScope, true));
    // [FOLLOW] El template elige pestana y estado vacio con estas dos banderas (AC-09).
    model.put("followingScope", followingScope);
    model.put("followsNobody", followingScope && followedIds.isEmpty());
    return new ModelAndView(viewName, model);
  }

  /** Tarjetas de la pagina: flag de guardado por card, sin volver a consultar agregados. */
  private static List<Map<String, Object>> feedCards(
    List<Plan> plans,
    Map<UUID, RatingSummary> summaries,
    Set<UUID> bookmarkedPlanIds
  ) {
    return plans
      .stream()
      .map(plan -> PlanFeedCards.planCard(plan, summaries, bookmarkedPlanIds.contains(plan.getId()))
      )
      .toList();
  }

  /** Agregados que necesita el ranking: solo popular los usa; fecha/nombre ordenan sin ellos. */
  private RankingStats rankingStats(PlanDiscoveryQuery query, List<Plan> candidates) {
    if (query.sort() != PlanDiscoveryQuery.Sort.POPULAR) {
      return new RankingStats(Map.of(), Map.of());
    }
    return new RankingStats(
      this.planRatingService.getSummariesByPlanId(),
      this.planService.getParticipantCounts(planIds(candidates))
    );
  }

  /** [PAGE-008] En popular se reutiliza el mapa del ranking; en el resto, solo la ventana. */
  private Map<UUID, RatingSummary> cardSummaries(
    PlanDiscoveryQuery query,
    Map<UUID, RatingSummary> rankingSummaries,
    List<UUID> pagePlanIds
  ) {
    if (query.sort() == PlanDiscoveryQuery.Sort.POPULAR) {
      return rankingSummaries;
    }
    return this.planRatingService.getSummariesByPlanId(pagePlanIds);
  }

  private static String currentEmail(Authentication authentication) {
    return authentication == null ? null : authentication.getName();
  }

  private static boolean ownedBy(Plan plan, String email) {
    return email != null && plan.isAdministrator(email);
  }

  private static boolean isFollowingScope(String scope) {
    return SCOPE_FOLLOWING.equalsIgnoreCase(scope == null ? "" : scope.trim());
  }

  private static String selectedCategory(PlanDiscoveryQuery query) {
    return query.category() == null ? "" : query.category().name();
  }

  /** Total de paginas, al menos una para que la pagina 1 de un feed vacio siga siendo valida. */
  private static int lastPage(int total, int size) {
    return Math.max(1, (int) (((long) total + size - 1) / size));
  }

  /** [PAGE] Ventana 1-based; el desborde de `page * size` no puede volverse un indice negativo. */
  private static List<Plan> pageOf(List<Plan> plans, int page, int size) {
    long offset = (long) (page - 1) * size;
    if (offset >= plans.size()) {
      return List.of();
    }
    int from = (int) offset;
    int to = (int) Math.min((long) plans.size(), offset + size);
    return plans.subList(from, to);
  }

  private static List<UUID> planIds(List<Plan> plans) {
    return plans.stream().map(Plan::getId).toList();
  }

  /**
   * [PAGE] URL del feed con la ventana y los filtros aplicados. {@code partial} agrega el parametro
   * que sirve solo las cards; sin el, el link es la pagina completa que funciona sin JavaScript.
   */
  private static String feedUrl(
    int page,
    PlanDiscoveryQuery query,
    boolean followingScope,
    boolean partial
  ) {
    UriComponentsBuilder url = UriComponentsBuilder
      .fromPath(FEED_PATH)
      .queryParam("page", page)
      .queryParam("when", query.when().name().toLowerCase(Locale.ROOT))
      .queryParam("sort", query.sort().name().toLowerCase(Locale.ROOT))
      .queryParam("size", query.size());
    if (!query.text().isEmpty()) {
      url.queryParam("q", query.text());
    }
    if (query.category() != null) {
      url.queryParam("category", query.category().name());
    }
    if (followingScope) {
      url.queryParam("scope", SCOPE_FOLLOWING);
    }
    if (partial) {
      url.queryParam("partial", "");
    }
    return url.build().encode().toUriString();
  }

  /** Id del visitante con sesion; en anonimo el principal es el String "anonymousUser". */
  private UUID viewerId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
      return null;
    }
    return userService.getByEmail(authentication.getName()).getId();
  }

  /** Visitante autenticado real: el principal anonimo es un String, no un {@link UserDetails}. */
  private static boolean signedIn(Authentication authentication) {
    return authentication != null && authentication.getPrincipal() instanceof UserDetails;
  }

  /**
   * [MARK] Planes guardados del visitante. Sin sesion no hay nada que preguntar y el toggle no se
   * renderiza (AC-05): el conjunto vacio no es un soft gate, es ausencia de control.
   */
  private Set<UUID> bookmarkedPlanIds(Authentication authentication) {
    return signedIn(authentication)
      ? this.planBookmarkService.getBookmarkedPlanIds(authentication.getName())
      : Set.of();
  }

  /** Los dos agregados que alimentan el ranking popular. */
  private record RankingStats(
    Map<UUID, RatingSummary> summaries,
    Map<UUID, Long> participantCounts
  ) {}
}
