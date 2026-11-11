package com.valhalla.domain.plan;

import com.valhalla.domain.place.PlaceCategory;
import java.util.Locale;

/**
 * [SEARCH] Filtros y orden del feed, ya normalizados. Los parametros crudos no llegan al dominio.
 *
 * @param text texto libre de busqueda
 * @param category categoria a filtrar, o {@code null} sin filtro
 * @param when ventana temporal (todas, proximas, pasadas)
 * @param sort orden del resultado
 * @param page pagina pedida, 1-based
 * @param size cantidad de planes por pagina
 */
public record PlanDiscoveryQuery(
  String text,
  PlaceCategory category,
  When when,
  Sort sort,
  int page,
  int size
) {
  public static final int DEFAULT_PAGE = 1;
  public static final int DEFAULT_SIZE = 12;

  public enum When {
    ALL,
    UPCOMING,
    PAST,
  }

  public enum Sort {
    POPULAR,
    DATE,
    NAME,
  }

  /** Un record construido a mano tambien cae a los defaults si la ventana es invalida. */
  public PlanDiscoveryQuery {
    if (!isValidPage(page)) {
      page = DEFAULT_PAGE;
    }
    if (!isValidSize(size)) {
      size = DEFAULT_SIZE;
    }
  }

  private static boolean isValidPage(int value) {
    return value >= DEFAULT_PAGE;
  }

  private static boolean isValidSize(int value) {
    return value >= 1;
  }

  /** Compatibilidad con el parseo anterior a [PAGE]: pagina 1, doce por pagina. */
  public static PlanDiscoveryQuery parse(String text, String category, String when, String sort) {
    return parse(text, category, when, sort, null, null);
  }

  /** Parseo tolerante: parametro vacio o invalido cae al default, nunca lanza. */
  public static PlanDiscoveryQuery parse(
    String text,
    String category,
    String when,
    String sort,
    String page,
    String size
  ) {
    return new PlanDiscoveryQuery(
      text == null ? "" : text.trim(),
      parseCategory(category),
      parseWhen(when),
      parseSort(sort),
      parsePositive(page, DEFAULT_PAGE),
      parsePositive(size, DEFAULT_SIZE)
    );
  }

  /** Hay filtro activo (el orden no cuenta como filtro): decide el estado vacio de AC-09. */
  public boolean hasFilters() {
    return !text.isEmpty() || category != null || when != When.ALL;
  }

  private static PlaceCategory parseCategory(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return PlaceCategory.fromCode(raw.trim().toUpperCase(Locale.ROOT)).orElse(null);
  }

  private static When parseWhen(String raw) {
    if (raw == null || raw.isBlank()) {
      return When.ALL;
    }
    return switch (raw.trim().toLowerCase(Locale.ROOT)) {
      case "upcoming" -> When.UPCOMING;
      case "past" -> When.PAST;
      default -> When.ALL;
    };
  }

  private static Sort parseSort(String raw) {
    if (raw == null || raw.isBlank()) {
      return Sort.POPULAR;
    }
    return switch (raw.trim().toLowerCase(Locale.ROOT)) {
      case "date" -> Sort.DATE;
      case "name" -> Sort.NAME;
      default -> Sort.POPULAR;
    };
  }

  /** Numero positivo; vacio, cero, negativo o no numerico cae al default sin lanzar. */
  private static int parsePositive(String raw, int fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      int value = Integer.parseInt(raw.trim());
      return value > 0 ? value : fallback;
    } catch (NumberFormatException invalid) {
      return fallback;
    }
  }
}
