package com.valhalla.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cache en memoria del catalogo de lugares. Es un cache de proceso: cada instancia de la app
 * tiene el suyo y no se coordina con las demas.
 *
 * <p>Se importa desde {@link BaseWebConfig} a proposito. {@code com.valhalla.config} no esta en
 * el component scan (que solo levanta {@code presentation} e {@code infrastructure}), asi que sin
 * el {@code @Import} la anotacion quedaria declarada y muerta, y los tests de integracion
 * correrian sin cache.
 *
 * <p>La escritura en el cache se posterga hasta el commit. Una lectura hecha dentro de una
 * transaccion es una foto que puede revertirse: los tests de integracion son transaccionales y
 * escriben por MockMvc, asi que cachear en el acto deja filas que la base nunca commiteo y el
 * resto de la suite lee un catalogo que no existe. La invalidacion, en cambio, es inmediata: una
 * escritura tiene que verse en la lectura siguiente aunque las dos compartan la transaccion.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    return new CommitAwareCacheManager();
  }

  private static final class CommitAwareCacheManager extends ConcurrentMapCacheManager {

    CommitAwareCacheManager() {
      super("places");
    }

    @Override
    protected Cache createConcurrentMapCache(String name) {
      return new CommitAwareCache(name);
    }
  }

  private static final class CommitAwareCache extends ConcurrentMapCache {

    CommitAwareCache(String name) {
      super(name);
    }

    @Override
    public void put(Object key, @Nullable Object value) {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              store(key, value);
            }
          }
        );
      } else {
        store(key, value);
      }
    }

    private void store(Object key, Object value) {
      super.put(key, value);
    }
  }
}
