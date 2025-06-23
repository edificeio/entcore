package org.entcore.portal.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.broker.api.dto.i18n.FetchTranslationsRequestDTO;
import org.entcore.broker.api.dto.i18n.FetchTranslationsResponseDTO;
import org.entcore.broker.api.dto.i18n.LangAndDomain;
import org.entcore.broker.api.dto.i18n.RegisterTranslationFilesRequestDTO;
import org.entcore.broker.api.dto.i18n.RegisterTranslationFilesResponseDTO;
import org.entcore.broker.proxy.I18nBrokerListener;
import org.entcore.common.cache.CacheService;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Implementation of the I18nBrokerListener interface.
 * This class handles internationalization operations received through the message broker.
 * It maintains a separate I18n instance for each application.
 */
public class I18nBrokerListenerImpl implements I18nBrokerListener {
    /**
     * Cache key format for storing i18n registration data
     */
    private static final String I18N_REGISTRATION_CACHE_KEY = "i18n:registration:%s";

    /**
     * Default application name used as fallback
     */
    private static final String DEFAULT_APPLICATION = "default";
    
    /**
     * Default TTL for cached i18n registration data (24 hours in seconds)
     */
    private static final int I18N_CACHE_TTL = 86400;
    private static final Logger log = LoggerFactory.getLogger(I18nBrokerListenerImpl.class);
    private final Map<String, I18n> i18nInstances = new HashMap<>();
    private final Vertx vertx;
    private final String assetsPath;
    private final CacheService cacheService;

    /**
     * Constructor for I18nBrokerListenerImpl.
     */
    public I18nBrokerListenerImpl() {
        this(Vertx.currentContext().owner(), "../..", CacheService.create(Vertx.currentContext().owner()));
    }

    /**
     * Constructor for I18nBrokerListenerImpl with dependencies.
     *
     * @param vertx The Vertx instance
     * @param assetsPath Path to the assets folder
     */
    public I18nBrokerListenerImpl(final Vertx vertx, final String assetsPath, final CacheService cacheService) {
        this(I18n.getInstance(), vertx, assetsPath, cacheService);
    }

    /**
     * Constructor for I18nBrokerListenerImpl with dependencies.
     *
     * @param defaultI18n The default I18n instance
     * @param vertx The Vertx instance
     * @param assetsPath Path to the assets folder
     */
    public I18nBrokerListenerImpl(final I18n defaultI18n, final Vertx vertx, final String assetsPath, final CacheService cacheService) {
        this.i18nInstances.put(DEFAULT_APPLICATION, defaultI18n);
        this.vertx = vertx;
        this.assetsPath = assetsPath;
        this.cacheService = cacheService;
    }

    /**
     * Loads I18n assets files for the specified application.
     * Uses a list of futures instead of a counter for better reliability.
     *
     * @param i18n The I18n instance to load files into
     * @param application The application name
     * @return Future that completes when loading is done
     */
    private Future<I18n> loadI18nAssetsFiles(final I18n i18n, final String application) {
        final Promise<I18n> promise = Promise.promise();
        final String i18nDirectory = assetsPath + File.separator + "i18n" + File.separator + application;

        // Check if the directory exists
        vertx.fileSystem().exists(i18nDirectory, existsResult -> {
            if (existsResult.succeeded() && existsResult.result()) {
                // List files in the directory
                vertx.fileSystem().readDir(i18nDirectory, readResult -> {
                    if (readResult.succeeded()) {
                        List<String> files = readResult.result();
                        if (files.isEmpty()) {
                            // No files to process, complete with current i18n
                            promise.complete(i18n);
                            return;
                        }

                        // Create a list of futures to track completion
                        final List<Future> fileFutures = new ArrayList<>();

                        for (String path : files) {
                            if (path.endsWith(".json")) {
                                // Create a promise for each file operation
                                Promise<Void> filePromise = Promise.promise();
                                fileFutures.add(filePromise.future());

                                // Read and load each JSON file
                                vertx.fileSystem().readFile(path, fileResult -> {
                                    if (fileResult.succeeded()) {
                                        try {
                                            final JsonObject i18nObject = new JsonObject(fileResult.result().toString());
                                            final String fileName = path.substring(path.lastIndexOf(File.separator) + 1);
                                            final String langCode = fileName.substring(0, fileName.lastIndexOf('.'));
                                            final Locale language = Locale.forLanguageTag(langCode);

                                            i18n.add(I18n.DEFAULT_DOMAIN, language, i18nObject);
                                            log.debug("Loaded I18n file for application {}, language: {}", application, language);
                                            filePromise.complete();
                                        } catch (Exception e) {
                                            log.error("Error loading I18n file: " + path, e);
                                            // Complete anyway but log the error
                                            filePromise.complete();
                                        }
                                    } else {
                                        log.error("Could not read I18n file: " + path, fileResult.cause());
                                        // Complete anyway but log the error
                                        filePromise.complete();
                                    }
                                });
                            }
                        }

                        // When all files are processed, complete the main promise
                        if (fileFutures.isEmpty()) {
                            promise.complete(i18n);
                        } else {
                            CompositeFuture.join(fileFutures)
                                .onComplete(ar -> promise.complete(i18n));
                        }
                    } else {
                        log.error("Could not read I18n directory: " + i18nDirectory, readResult.cause());
                        promise.fail(readResult.cause());
                    }
                });
            } else {
                if (existsResult.failed()) {
                    log.error("Error checking I18n directory: " + i18nDirectory, existsResult.cause());
                } else {
                    log.warn("I18n directory does not exist for application {}: {}", application, i18nDirectory);
                }
                promise.complete(i18n);
            }
        });

        return promise.future();
    }

    /**
     * Loads I18n theme files for the specified application.
     * Returns a Future that completes when loading is done.
     *
     * @param i18n The I18n instance to load files into
     * @param application The application name
     * @return Future that completes when loading is done
     */
    private Future<I18n> loadI18nThemesFiles(final I18n i18n, final String application) {
        final Promise<I18n> promise = Promise.promise();
        final String themesDirectory = assetsPath + File.separator + "themes";
        final Map<String, String> skins = vertx.sharedData().getLocalMap("skins");
        final Map<String, String> reverseSkins = new HashMap<>();

        // Create reverse mapping of skins
        for (Map.Entry<String, String> e : skins.entrySet()) {
            reverseSkins.put(e.getValue(), e.getKey());
        }

        vertx.fileSystem().exists(themesDirectory, event -> {
            if (event.succeeded() && event.result()) {
                vertx.fileSystem().readDir(themesDirectory, themes -> {
                    if (themes.succeeded()) {
                        List<String> themesList = themes.result();
                        if (themesList.isEmpty()) {
                            // No themes to process, complete successfully
                            promise.complete(i18n);
                            return;
                        }

                        // Track completion of all theme processing
                        final List<Future<Void>> themeFutures = new ArrayList<>();

                        for (String theme : themesList) {
                            final String themeName = theme.substring(theme.lastIndexOf(File.separator) + 1);
                            final String domain = reverseSkins.get(themeName);

                            if (domain == null) {
                                log.debug("Missing domain for theme: {}", themeName);
                                continue;
                            }

                            final String i18nDirectory = theme + File.separator + "i18n" + File.separator + application;
                            themeFutures.add(readI18nForTheme(i18n, domain, i18nDirectory, themeName));
                        }

                        // When all themes are processed, complete the promise
                        CompositeFuture.all(new ArrayList<>(themeFutures))
                            .onComplete(ar -> {
                                if (!ar.succeeded()) {
                                    log.error("Error processing themes", ar.cause());
                                    // Still complete with i18n instance, even if some themes failed
                                }
                                promise.complete(i18n);
                            });
                    } else {
                        log.error("Error listing themes directory", themes.cause());
                        promise.complete(i18n); // Still return i18n instance
                    }
                });
            } else {
                log.debug("Themes directory does not exist: {}", themesDirectory);
                promise.complete(i18n); // Still return i18n instance
            }
        });

        return promise.future();
    }

    /**
     * Reads and loads I18n files for a specific theme.
     * Uses a list of futures instead of a counter.
     *
     * @param i18n The I18n instance to load files into
     * @param domain The domain for the I18n files
     * @param i18nDirectory The directory containing I18n files
     * @param themeName The name of the theme
     * @return Future that completes when loading is done
     */
    private Future<Void> readI18nForTheme(final I18n i18n, final String domain, final String i18nDirectory, final String themeName) {
        Promise<Void> promise = Promise.promise();

        vertx.fileSystem().exists(i18nDirectory, ar -> {
            if (ar.succeeded() && ar.result()) {
                vertx.fileSystem().readDir(i18nDirectory, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        List<String> files = asyncResult.result();
                        if (files.isEmpty()) {
                            promise.complete(); // No files to process
                            return;
                        }

                        // Create a list of futures to track completion
                        final List<Future<?>> fileFutures = new ArrayList<>();

                        for (final String s : files) {
                            if (s.endsWith(".json")) {
                                // Create a promise for each file operation
                                Promise<Void> filePromise = Promise.promise();
                                fileFutures.add(filePromise.future());

                                final String fileName = s.substring(s.lastIndexOf(File.separator) + 1);
                                final String langCode = fileName.substring(0, fileName.lastIndexOf('.'));
                                final Locale locale = Locale.forLanguageTag(langCode);

                                vertx.fileSystem().readFile(s, fileResult -> {
                                    if (fileResult.succeeded()) {
                                        try {
                                            final JsonObject i18nObject = new JsonObject(fileResult.result().toString());
                                            i18n.add(domain, locale, i18nObject);
                                            // Also add under theme name for compatibility
                                            i18n.add(themeName, locale, i18nObject, true);
                                            log.debug("Loaded theme I18n file: {} for theme: {}, locale: {}", s, themeName, locale);
                                        } catch (Exception e) {
                                            log.error("Error loading theme I18n file: " + s, e);
                                        }
                                    } else {
                                        log.error("Could not read theme I18n file: " + s, fileResult.cause());
                                    }
                                    filePromise.complete();
                                });
                            }
                        }

                        // When all files are processed, complete the main promise
                        if (fileFutures.isEmpty()) {
                            promise.complete();
                        } else {
                            Future.join(fileFutures)
                                .onComplete(onComplete -> promise.complete());
                        }
                    } else {
                        log.error("Error reading theme I18n directory: " + i18nDirectory, asyncResult.cause());
                        promise.fail(asyncResult.cause());
                    }
                });
            } else {
                log.debug("Theme I18n directory does not exist: {}", i18nDirectory);
                promise.complete(); // Nothing to do, complete successfully
            }
        });

        return promise.future();
    }

    /**
     * Fetches translations based on the provided request.
     * If the application's translations are not loaded, attempts to recover from cache.
     *
     * @param request The request containing application, headers or language and domain
     * @return A Future containing the response with translations
     */
    @Override
    public Future<FetchTranslationsResponseDTO> fetchTranslations(final FetchTranslationsRequestDTO request) {
        // Validate the request
        if (request == null || !request.isValid()) {
            log.error("Invalid fetchTranslations request: request is null or invalid {}", request);
            return Future.failedFuture("i18n.parameters.invalid");
        }

        try {
            // Get the application from the request, or use "default" if not specified
            final String application = (request.getApplication() == null || request.getApplication().isEmpty())
                    ? DEFAULT_APPLICATION
                    : request.getApplication();

            // Check if the application's I18n instance is already loaded
            final I18n appI18n = i18nInstances.get(application);

            if (appI18n != null) {
                // Application already loaded, proceed with fetching
                log.debug("Using existing I18n instance for application: {}", application);
                return getTranslations(request, appI18n, application);
            }

            // Application not loaded, try to recover from cache
            log.debug("Application {} not loaded, attempting to recover from cache", application);
            
            // Attempt to restore from cache and then fetch translations
            return tryRestoreFromCache(application)
                    .compose(restored -> {
                        if (restored) {
                            // Successfully restored, try fetching again
                            log.debug("Successfully restored {} from cache, fetching translations", application);
                            return fetchTranslations(request);
                        } else {
                            // Failed to restore, fall back to default
                            log.warn("Could not restore {} from cache, falling back to default", application);
                            return fallbackToDefaultInstance(request, application);
                        }
                    });
                    
        } catch (Exception e) {
            log.error("Error fetching translations", e);
            return Future.failedFuture("i18n.fetch.error");
        }
    }

    /**
     * Attempts to restore application translations from cache
     * 
     * @param application The application name to restore
     * @return Future with boolean indicating success (true) or failure (false)
     */
    private Future<Boolean> tryRestoreFromCache(String application) {
        Promise<Boolean> promise = Promise.promise();
        final String cacheKey = String.format(I18N_REGISTRATION_CACHE_KEY, application);
        
        // First, retrieve data from cache
        retrieveRegistrationDataFromCache(cacheKey)
            .onComplete(ar -> {
                if (ar.succeeded() && ar.result() != null) {
                    // Found data in cache, try to restore it
                    RegisterTranslationFilesRequestDTO cachedRequest = ar.result();
                    registerI18nFiles(cachedRequest)
                        .onComplete(registerAr -> {
                            if (registerAr.succeeded()) {
                                log.info("Successfully restored I18n from cache for application: {}", application);
                                promise.complete(true);
                            } else {
                                log.error("Failed to restore I18n from cache for application: {}", application, registerAr.cause());
                                promise.complete(false);
                            }
                        });
                } else {
                    // No data in cache or error retrieving
                    log.debug("No cached registration data found for application: {}", application);
                    promise.complete(false);
                }
            });

        return promise.future();
    }

    /**
     * Retrieves registration data from cache
     * 
     * @param cacheKey The cache key to retrieve data from
     * @return Future with RegisterTranslationFilesRequestDTO if found, or null if not found
     */
    private Future<RegisterTranslationFilesRequestDTO> retrieveRegistrationDataFromCache(String cacheKey) {
        Promise<RegisterTranslationFilesRequestDTO> promise = Promise.promise();
        
        cacheService.get(cacheKey, cacheResult -> {
            if (cacheResult.succeeded() && cacheResult.result().isPresent()) {
                try {
                    // Use ObjectMapper through RegisterTranslationFilesRequestDTO.fromJSON
                    RegisterTranslationFilesRequestDTO cachedRequest = 
                            RegisterTranslationFilesRequestDTO.fromJSON(cacheResult.result().get());
                    promise.complete(cachedRequest);
                } catch (Exception e) {
                    log.error("Error deserializing cached registration data", e);
                    promise.complete(null);
                }
            } else {
                // Not found in cache
                promise.complete(null);
            }
        });
        
        return promise.future();
    }

    /**
     * Fall back to default I18n instance for translations
     * 
     * @param request The original request
     * @param application The application that was not found
     * @return Future with the translations response
     */
    private Future<FetchTranslationsResponseDTO> fallbackToDefaultInstance(FetchTranslationsRequestDTO request, String application) {
        // Fall back to default instance
        final I18n defaultI18n = i18nInstances.get(DEFAULT_APPLICATION);
        if (defaultI18n == null) {
            return Future.failedFuture("i18n.application.not.registered");
        }
        
        log.warn("Using default I18n instance as fallback for application: {}", application);
        return getTranslations(request, defaultI18n, DEFAULT_APPLICATION);
    }

    /**
     * Helper method for getting translations - overloaded without promise parameter
     */
    private Future<FetchTranslationsResponseDTO> getTranslations(
            final FetchTranslationsRequestDTO request,
            final I18n i18n,
            final String application) {
        final Promise<FetchTranslationsResponseDTO> promise = Promise.promise();

        final JsonObject translations = new JsonObject();

        try {
            // Use headers or explicit language and domain
            if (request.getHeaders() != null) {
                // Create a simulated HttpServerRequest with the provided headers
                final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
                request.getHeaders().forEach((key, value) -> {
                    if (value != null) {
                        headers.add(key, value);
                    }
                });

                final HttpServerRequest httpRequest = new JsonHttpServerRequest(new JsonObject(), headers);
                final JsonObject translationsForRequest = i18n.load(httpRequest);
                if(translationsForRequest != null) {
                    translations.mergeIn(translationsForRequest);
                } else {
                    log.debug("No translations found for headers in application " + application);
                }
            } else {
                // Use explicit language and domain
                final LangAndDomain langAndDomain = request.getLangAndDomain();
                translations.mergeIn(langAndDomain.getDomain() == null ?
                        i18n.load(langAndDomain.getLang()) :
                        i18n.load(langAndDomain.getLang(), langAndDomain.getDomain())
                );
            }

            // Convert JsonObject to Map<String, String>
            final Map<String, String> translationsMap = new HashMap<>();
            for (String key : translations.fieldNames()) {
                Object value = translations.getValue(key);
                // Only include string values
                if (value instanceof String) {
                    translationsMap.put(key, (String) value);
                }
            }

            // Create and complete the response
            final FetchTranslationsResponseDTO response = new FetchTranslationsResponseDTO(translationsMap);
            promise.complete(response);

            log.debug("Successfully fetched " + translationsMap.size() + " translations for application " + application);
        } catch (Exception e) {
            log.error("Error processing translations for application: " + application, e);
            promise.fail("i18n.processing.error");
        }

        return promise.future();
    }

    /**
     * Registers I18n files and stores them in cache for recovery
     */
    @Override
    public Future<RegisterTranslationFilesResponseDTO> registerI18nFiles(final RegisterTranslationFilesRequestDTO request) {
        // Validate request
        if (request == null || !request.isValid()) {
            log.error("Invalid registerI18nFiles request: {}", request);
            return Future.failedFuture("i18n.register.invalid.request");
        }

        try {
            final String application = request.getApplication();
            
            // Process registration
            Future<RegisterTranslationFilesResponseDTO> registrationFuture = processI18nRegistration(request);
            
            // Store in cache after successful registration
            registrationFuture.onSuccess(response -> 
                storeRegistrationDataInCache(request)
                    .onFailure(err -> log.warn("Failed to cache registration data for {}, but registration succeeded", application, err))
            );
            
            return registrationFuture;
        } catch (Exception e) {
            log.error("Error registering I18n files", e);
            return Future.failedFuture("i18n.register.error: " + e.getMessage());
        }
    }

    /**
     * Process the I18n registration without cache operations
     * 
     * @param request The registration request
     * @return Future with the registration response
     */
    private Future<RegisterTranslationFilesResponseDTO> processI18nRegistration(RegisterTranslationFilesRequestDTO request) {
        Promise<RegisterTranslationFilesResponseDTO> promise = Promise.promise();
        
        final String application = request.getApplication();
        final Map<String, Map<String, String>> translationsByLanguage = request.getTranslationsByLanguage();

        log.info("Registering I18n dictionaries for application: {}", application);

        // Create a new I18n instance
        final I18n i18n = new I18n().initializeMessages(Collections.emptyMap());

        int translationsCount = 0;

        // Register translations from the map
        for (Map.Entry<String, Map<String, String>> entry : translationsByLanguage.entrySet()) {
            final String filename = entry.getKey();
            final Map<String, String> translations = entry.getValue();

            // Extract language code from filename (e.g. "fr.json" -> "fr")
            final String langCode = filename.substring(0, filename.lastIndexOf('.'));
            final Locale locale = Locale.forLanguageTag(langCode);

            // Convert Map to JsonObject
            final JsonObject i18nObject = new JsonObject();
            translations.forEach(i18nObject::put);

            // Add translations to the I18n instance
            i18n.add(I18n.DEFAULT_DOMAIN, locale, i18nObject);
            translationsCount += translations.size();

            log.debug("Registered {} translations for language {} in application {}",
                    translations.size(), langCode, application);
        }

        // Final count for the response
        final int finalTranslationsCount = translationsCount;

        // Chain loading of assets files and then theme files with proper error handling
        Future<I18n> assetsFuture = loadI18nAssetsFiles(i18n, application);

        assetsFuture.compose(
                updatedI18n -> loadI18nThemesFiles(updatedI18n, application),
                Future::failedFuture
        ).onComplete(ar -> {
            if (ar.succeeded()) {
                // Store the fully loaded instance
                i18nInstances.put(application, ar.result());

                // Create and return response
                final RegisterTranslationFilesResponseDTO response = new RegisterTranslationFilesResponseDTO(
                        application,
                        translationsByLanguage.size(),
                        finalTranslationsCount);

                log.info("Successfully registered I18n for application {}: {} languages, {} translations",
                        application, translationsByLanguage.size(), finalTranslationsCount);

                promise.complete(response);
            } else {
                log.error("Error loading I18n files for application: " + application, ar.cause());
                promise.fail("i18n.files.load.error: " + ar.cause().getMessage());
            }
        });
        
        return promise.future();
    }

    /**
     * Stores the registration data in cache for potential recovery
     * 
     * @param request The registration request to store
     * @return Future that completes when caching is done
     */
    private Future<Void> storeRegistrationDataInCache(RegisterTranslationFilesRequestDTO request) {
        Promise<Void> promise = Promise.promise();
        
        try {
            final String application = request.getApplication();
            final String cacheKey = String.format(I18N_REGISTRATION_CACHE_KEY, application);
            
            // Serialize using RegisterTranslationFilesRequestDTO.toJSON
            String jsonData;
            try {
                jsonData = request.toJSON();
            } catch (JsonProcessingException e) {
                log.error("Error serializing registration data for application: {}", application, e);
                promise.fail(e);
                return promise.future();
            }
            
            // Store in cache with TTL
            cacheService.upsert(cacheKey, jsonData, I18N_CACHE_TTL, result -> {
                if (result.succeeded()) {
                    log.debug("Successfully cached registration data for application: {}", application);
                    promise.complete();
                } else {
                    log.warn("Failed to cache registration data for application: {}", application, result.cause());
                    promise.fail(result.cause());
                }
            });
        } catch (Exception e) {
            log.error("Error storing registration data in cache", e);
            promise.fail(e);
        }
        
        return promise.future();
    }
}