package com.valhalla.presentation.plan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import com.valhalla.domain.place.Place;
import com.valhalla.domain.plan.Plan;
import com.valhalla.domain.planplace.PlanPlace;
import jakarta.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * [OG-JPG] La cadena de prioridad del preview (foto de lugar > portada SVG > imagen de marca)
 * y cada bajada de nivel: itinerary sin fotos raster, url no http, 404, content-type que no es
 * imagen, descarga gigante, bytes indecodificables, host inalcanzable y plan privado cuyo
 * lazy lanza. Corre contra un HttpServer real porque el contrato con los scrapers es que el
 * endpoint SIEMPRE devuelve un JPEG canvas o 404, nunca algo a medio romper.
 */
public class PlanOgImageServiceTest {

  private static final UUID PLAN_ID = UUID.randomUUID();

  private HttpServer server;
  private int port;

  @BeforeEach
  public void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    port = server.getAddress().getPort();
    server.start();
  }

  @AfterEach
  public void stopServer() {
    server.stop(0);
  }

  private void serve(String path, int status, String contentType, byte[] body) {
    server.createContext(
      path,
      exchange -> {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        if (status != 200) {
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
          return;
        }
        exchange.sendResponseHeaders(200, body.length);
        try (var out = exchange.getResponseBody()) {
          out.write(body);
        }
      }
    );
  }

  private static byte[] png(int width, int height, boolean noisy) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    if (noisy) {
      Random random = new Random(42);
      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          image.setRGB(x, y, random.nextInt(0x1000000));
        }
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  private Plan photoPlan(String... imageUrls) {
    Plan plan = mock(Plan.class);
    lenient().when(plan.getId()).thenReturn(PLAN_ID);
    List<PlanPlace> planPlaces = Arrays
      .stream(imageUrls)
      .map(url -> {
        Place place = mock(Place.class);
        lenient().when(place.getImageUrl()).thenReturn(url);
        PlanPlace planPlace = mock(PlanPlace.class);
        lenient().when(planPlace.getPlace()).thenReturn(place);
        return planPlace;
      })
      .toList();
    when(plan.getPlanPlaces()).thenReturn(planPlaces);
    return plan;
  }

  /** Plan cuyo itinerario lanza por lazy: el camino de todo plan privado sin calentar. */
  private Plan coverOnlyPlan() {
    Plan plan = mock(Plan.class);
    lenient().when(plan.getId()).thenReturn(PLAN_ID);
    when(plan.getPlanPlaces()).thenThrow(new RuntimeException("failed to lazily initialize"));
    return plan;
  }

  private ServletContext contextWith(Map<String, byte[]> resources) {
    ServletContext context = mock(ServletContext.class);
    lenient()
      .when(context.getResourceAsStream(anyString()))
      .thenAnswer(invocation -> {
        byte[] content = resources.get(invocation.getArgument(0));
        return content == null ? null : new ByteArrayInputStream(content);
      });
    return context;
  }

  private Map<String, byte[]> brandResources() throws IOException {
    return new HashMap<>(Map.of("/resources/images/og-image.png", png(1200, 630, false)));
  }

  private ServletContext contextWithBrand() throws IOException {
    return contextWith(brandResources());
  }

  private ServletContext contextWithCover() throws IOException {
    Map<String, byte[]> resources = brandResources();
    resources.put(
      "/resources" + PlanFeedCards.coverFor(PLAN_ID),
      ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1200 640\">" +
        "<rect width=\"1200\" height=\"640\" fill=\"#223344\"/></svg>").getBytes(
          StandardCharsets.UTF_8
        )
    );
    return contextWith(resources);
  }

  /** Decodifica y fija el contrato 1200x630 que prometen los metas og:image:width/height. */
  private void assertCanvasJpeg(Optional<byte[]> result) throws IOException {
    assertThat(result.isPresent(), is(true));
    BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.get()));
    assertThat(decoded, notNullValue());
    assertThat(decoded.getWidth(), is(1200));
    assertThat(decoded.getHeight(), is(630));
  }

  @Test
  public void theFirstPlacePhotoBecomesThePreview() throws Exception {
    serve("/foto.png", 200, "image/png", png(320, 200, false));
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/foto.png");

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void onlyRasterPhotoUrlsQualifyAndTheFirstWins() throws Exception {
    // null, vacia y .svg se descartan; la primera valida se pide (404 aqui) y baja a portada.
    Plan plan = photoPlan(
      null,
      "   ",
      "https://cdn.example.com/portada.svg",
      "http://127.0.0.1:" + port + "/no-existe.jpg"
    );
    serve("/no-existe.jpg", 404, "image/jpeg", new byte[0]);

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void aLazyPrivateItineraryUsesTheCoverWithoutTouchingThePlaces() throws Exception {
    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(coverOnlyPlan()));
  }

  @Test
  public void nonHttpPhotoUrlsAreNeverFetched() throws Exception {
    Plan plan = photoPlan("ftp://cdn.example.com/foto.jpg");

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void aNonImageContentTypeFallsBackToTheCover() throws Exception {
    serve(
      "/foto",
      200,
      "text/html",
      "<html>no es una foto</html>".getBytes(StandardCharsets.UTF_8)
    );
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/foto");

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void anUndecodableBodyFallsBackToTheCover() throws Exception {
    // Content-type image/png pero bytes que no son ninguna imagen: ImageIO.read da null.
    serve("/foto.png", 200, "image/png", "no soy png".getBytes(StandardCharsets.UTF_8));
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/foto.png");

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void anUnreachableHostFallsBackToTheCover() throws Exception {
    // Puerto 1: la conexion lanza IOException contra el servidor.
    Plan plan = photoPlan("http://127.0.0.1:1/foto.png");

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void anOversizedDownloadFallsBackToTheCover() throws Exception {
    // MAX_DOWNLOAD_BYTES es 8_000_000; un byte mas tira el recurso antes de tocar memoria.
    serve("/grande.png", 200, "image/png", new byte[8_000_001]);
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/grande.png");

    assertCanvasJpeg(new PlanOgImageService(contextWithCover()).ogImageFor(plan));
  }

  @Test
  public void theBrandImageIsTheLastCover() throws Exception {
    // Contexto sin portada SVG: la foto falla y el preview cae al escalon de marca.
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/na.jpg");
    serve("/na.jpg", 404, "image/jpeg", new byte[0]);

    assertCanvasJpeg(new PlanOgImageService(contextWithBrand()).ogImageFor(plan));
  }

  @Test
  public void withoutCoverOrBrandThePreviewIsEmpty() throws Exception {
    Optional<byte[]> result = new PlanOgImageService(contextWith(Map.of()))
      .ogImageFor(coverOnlyPlan());
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void aCorruptBrandStreamYieldsNoPreviewInsteadOfAnError() throws Exception {
    ServletContext context = mock(ServletContext.class);
    // Un stream que da EOF y explota al close: ImageIO.read hace null con un stream que lanza
    // durante el sniffing (los readers se comen ese IOException), asi que el punto de fallo
    // determinista es el try-with-resources, que si propaga el IOException al catch.
    when(context.getResourceAsStream(anyString())).thenReturn(brokenCloseStream());
    when(context.getResourceAsStream(startsWith("/resources/images/plans/"))).thenReturn(null);

    assertThat(new PlanOgImageService(context).ogImageFor(coverOnlyPlan()).isEmpty(), is(true));
  }

  private static InputStream brokenCloseStream() {
    return new InputStream() {
      @Override
      public int read() {
        return -1;
      }

      @Override
      public void close() throws IOException {
        throw new IOException("cierre roto");
      }
    };
  }

  @Test
  public void aCorruptCoverStreamBlamesTheCoverInTheMessage() throws Exception {
    ServletContext context = mock(ServletContext.class);
    when(context.getResourceAsStream(anyString())).thenReturn(throwingStream());

    try {
      new PlanOgImageService(context).ogImageFor(coverOnlyPlan());
      throw new AssertionError("esperaba IllegalStateException");
    } catch (IllegalStateException expected) {
      assertThat(
        expected.getMessage(),
        is("No se pudo convertir la portada " + PlanFeedCards.coverFor(PLAN_ID))
      );
    }
  }

  private static InputStream throwingStream() {
    return new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("stream roto");
      }
    };
  }

  @Test
  public void encodeFailuresWalkDownThePriorityChain() throws Exception {
    serve("/foto.png", 200, "image/png", png(320, 200, false));
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/foto.png");
    // El contexto se arma antes del static mock: armar el PNG de prueba usa ImageIO.write,
    // que con el mock encima rebotaria contra el encoder roto que estamos inyectando.
    ServletContext context = contextWithCover();

    ImageWriter failing = mock(ImageWriter.class);
    when(failing.getDefaultWriteParam()).thenReturn(mock(ImageWriteParam.class));
    doThrow(new IOException("encoder roto"))
      .when(failing)
      .write(nullable(IIOMetadata.class), any(IIOImage.class), nullable(ImageWriteParam.class));
    try (MockedStatic<ImageIO> imageIO = mockStatic(ImageIO.class, CALLS_REAL_METHODS)) {
      imageIO
        .when(() -> ImageIO.getImageWritersByFormatName("jpeg"))
        .thenAnswer(invocation -> Collections.singletonList(failing).iterator());

      // Foto y portada comparten el encoder roto: la foto baja a portada y la portada a empty.
      assertThat(new PlanOgImageService(context).ogImageFor(plan).isEmpty(), is(true));
    }
  }

  @Test
  public void theLastQualityTierIsTakenWhenNoTierFitsTheCap() throws Exception {
    // Ruido puro: ningun tramo de calidad entra bajo 250 KB y el servicio igual entrega el
    // ultimo antes que rendirse con un 404 para el scraper.
    serve("/ruido.png", 200, "image/png", png(1200, 630, true));
    Plan plan = photoPlan("http://127.0.0.1:" + port + "/ruido.png");

    Optional<byte[]> result = new PlanOgImageService(contextWithCover()).ogImageFor(plan);
    assertThat(result.isPresent(), is(true));
    // No se asserta <= 250 KB: en este extremo se entrega el ultimo tramo aunque pase el tope;
    // si algun dia entrara, la linea del tope seguiria cubierta por el resto de la cadena.
    assertThat(result.get().length > 0, is(true));
  }

  @Test
  public void theResultIsCachedPerPhotoAndPerCover() throws Exception {
    serve("/foto.png", 200, "image/png", png(320, 200, false));
    PlanOgImageService service = new PlanOgImageService(contextWithCover());

    byte[] first = service
      .ogImageFor(photoPlan("http://127.0.0.1:" + port + "/foto.png"))
      .orElseThrow();
    // Misma clave, mismos bytes: el scraper que vuelve no recalcula nada.
    assertThat(
      service.ogImageFor(photoPlan("http://127.0.0.1:" + port + "/foto.png")).orElseThrow(),
      is(first)
    );

    serve("/otra.png", 200, "image/png", png(200, 120, false));
    assertThat(
      service.ogImageFor(photoPlan("http://127.0.0.1:" + port + "/otra.png")).isPresent(),
      is(true)
    );
  }
}
