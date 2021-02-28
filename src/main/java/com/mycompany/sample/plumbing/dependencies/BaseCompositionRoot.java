package com.mycompany.sample.plumbing.dependencies;

import java.util.function.Supplier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import com.mycompany.sample.plumbing.claims.ClaimsCache;
import com.mycompany.sample.plumbing.claims.ClaimsSupplier;
import com.mycompany.sample.plumbing.claims.CoreApiClaims;
import com.mycompany.sample.plumbing.claims.CustomClaimsProvider;
import com.mycompany.sample.plumbing.configuration.ClaimsConfiguration;
import com.mycompany.sample.plumbing.configuration.LoggingConfiguration;
import com.mycompany.sample.plumbing.configuration.OAuthConfiguration;
import com.mycompany.sample.plumbing.logging.LoggerFactory;
import com.mycompany.sample.plumbing.oauth.IssuerMetadata;

/*
 * A class to manage composing core API behaviour
 */
@SuppressWarnings("PMD.GenericsNaming")
public final class BaseCompositionRoot<TClaims extends CoreApiClaims> {

    private final ConfigurableListableBeanFactory container;
    private LoggingConfiguration loggingConfiguration;
    private LoggerFactory loggerFactory;
    private OAuthConfiguration oauthConfiguration;
    private ClaimsConfiguration claimsConfiguration;
    private Supplier<TClaims> claimsSupplier;
    private Supplier<CustomClaimsProvider<TClaims>> customClaimsProviderSupplier;

    public BaseCompositionRoot(final ConfigurableListableBeanFactory container) {
        this.container = container;
        this.claimsSupplier = null;
        this.customClaimsProviderSupplier = null;
    }

    /*
     * Receive the logging configuration so that we can create objects related to logging and error handling
     */
    public BaseCompositionRoot<TClaims> useDiagnostics(
            final LoggingConfiguration loggingConfiguration,
            final LoggerFactory loggerFactory) {

        this.loggingConfiguration = loggingConfiguration;
        this.loggerFactory = loggerFactory;
        return this;
    }

    /*
     * Indicate that we're using OAuth and receive the configuration
     */
    public BaseCompositionRoot<TClaims> useOAuth(final OAuthConfiguration oauthConfiguration) {

        this.oauthConfiguration = oauthConfiguration;
        return this;
    }

    /*
     * Receive information used for claims caching
     */
    public BaseCompositionRoot<TClaims> useClaimsCaching(final ClaimsConfiguration claimsConfiguration) {

        this.claimsConfiguration = claimsConfiguration;
        return this;
    }

    /*
     * Consumers must provide a callback for creating claims
     */
    public BaseCompositionRoot<TClaims> withClaimsSupplier(final Supplier<TClaims> claimsSupplier) {
        this.claimsSupplier = claimsSupplier;
        return this;
    }

    /*
     * Consumers can provide a callback for creating a custom claims provider
     */
    public BaseCompositionRoot<TClaims> withCustomClaimsProviderSupplier(
            final Supplier<CustomClaimsProvider<TClaims>> supplier) {

        this.customClaimsProviderSupplier = supplier;
        return this;
    }

    /*
     * Register and return the authorizer
     */
    public void register() {

        // Register dependencies for logging and error handling
        this.registerBaseDependencies();

        // Register OAuth specific dependencies for Entry Point APIs
        if (this.oauthConfiguration != null) {
            this.registerOAuthDependencies();
        }

        // Register claims dependencies for all APIs
        this.registerClaimsDependencies();
    }

    /*
     * Register dependencies used for logging and error handling, which are natural singletons
     */
    private void registerBaseDependencies() {

        this.container.registerSingleton("LoggingConfiguration", this.loggingConfiguration);
        this.container.registerSingleton("LoggerFactory", this.loggerFactory);
    }

    /*
     * Register dependencies used for OAuth processing
     */
    private void registerOAuthDependencies() {

        // Load metadata if using OAuth security
        var metadata = new IssuerMetadata(this.oauthConfiguration);
        metadata.initialize();

        // Register these natural singletons
        this.container.registerSingleton("OAuthConfiguration", this.oauthConfiguration);
        this.container.registerSingleton("IssuerMetadata", metadata);
    }

    /*
     * Register dependencies used for Claims processing
     */
    private void registerClaimsDependencies() {

        // Create an injectable object to allow claims objects of a concrete type to be created at runtime
        var supplier = new ClaimsSupplier<TClaims>() {

            @Override
            public TClaims createEmptyClaims() {
                return claimsSupplier.get();
            }

            @Override
            public CustomClaimsProvider<TClaims> createCustomClaimsProvider() {
                return customClaimsProviderSupplier.get();
            }
        };

        // Create and initialize the claims cache
        var cache = new ClaimsCache<TClaims>(this.claimsConfiguration, this.loggerFactory);
        cache.initialize();

        // Register these natural singletons
        this.container.registerSingleton("ClaimsCache", cache);
        this.container.registerSingleton("ClaimsSupplier", supplier);
    }
}