/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fireflyframework.idp.internaldb.config;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.idp.adapter.IdpAdapter;
import org.fireflyframework.idp.internaldb.adapter.InternalDbIdpAdapter;
import org.fireflyframework.idp.internaldb.repository.RefreshTokenRepository;
import org.fireflyframework.idp.internaldb.repository.RoleRepository;
import org.fireflyframework.idp.internaldb.repository.SessionRepository;
import org.fireflyframework.idp.internaldb.repository.UserRepository;
import org.fireflyframework.idp.internaldb.repository.UserRoleRepository;
import org.fireflyframework.idp.internaldb.service.AuthenticationService;
import org.fireflyframework.idp.internaldb.service.JwtTokenService;
import org.fireflyframework.idp.internaldb.service.RoleService;
import org.fireflyframework.idp.internaldb.service.UserManagementService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring auto-configuration for Internal Database IDP Adapter.
 *
 * <p>This configuration class:
 * <ul>
 *   <li>Enables Internal DB IDP configuration properties</li>
 *   <li>Enables R2DBC repositories</li>
 *   <li>Configures BCrypt password encoder</li>
 *   <li>Registers all service beans and the IDP adapter</li>
 *   <li>Is automatically loaded when provider=internal-db and R2DBC is on the classpath</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "firefly.idp.provider", havingValue = "internal-db")
@ConditionalOnClass(ConnectionFactory.class)
@EnableConfigurationProperties(InternalDbProperties.class)
@EnableR2dbcRepositories(basePackages = "org.fireflyframework.idp.internaldb.repository")
@Slf4j
public class InternalDbIdpConfiguration {

    public InternalDbIdpConfiguration() {
        log.info("Internal Database IDP Adapter Configuration loaded");
    }

    /**
     * Configure BCrypt password encoder with strength 12.
     *
     * @return the password encoder bean
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        log.info("Configuring BCryptPasswordEncoder with strength 12");
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configure JWT token service.
     *
     * @param properties the internal DB properties
     * @return the JWT token service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtTokenService jwtTokenService(InternalDbProperties properties) {
        return new JwtTokenService(properties);
    }

    /**
     * Configure role service.
     *
     * @param roleRepository the role repository
     * @param userRoleRepository the user role repository
     * @return the role service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RoleService roleService(RoleRepository roleRepository,
                                   UserRoleRepository userRoleRepository) {
        return new RoleService(roleRepository, userRoleRepository);
    }

    /**
     * Configure user management service.
     *
     * @param userRepository the user repository
     * @param userRoleRepository the user role repository
     * @param sessionRepository the session repository
     * @param refreshTokenRepository the refresh token repository
     * @param passwordEncoder the password encoder
     * @return the user management service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public UserManagementService userManagementService(UserRepository userRepository,
                                                       UserRoleRepository userRoleRepository,
                                                       SessionRepository sessionRepository,
                                                       RefreshTokenRepository refreshTokenRepository,
                                                       PasswordEncoder passwordEncoder) {
        return new UserManagementService(userRepository, userRoleRepository, sessionRepository,
                refreshTokenRepository, passwordEncoder);
    }

    /**
     * Configure authentication service.
     *
     * @param userRepository the user repository
     * @param sessionRepository the session repository
     * @param refreshTokenRepository the refresh token repository
     * @param roleService the role service
     * @param jwtTokenService the JWT token service
     * @param passwordEncoder the password encoder
     * @param properties the internal DB properties
     * @return the authentication service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthenticationService authenticationService(UserRepository userRepository,
                                                       SessionRepository sessionRepository,
                                                       RefreshTokenRepository refreshTokenRepository,
                                                       RoleService roleService,
                                                       JwtTokenService jwtTokenService,
                                                       PasswordEncoder passwordEncoder,
                                                       InternalDbProperties properties) {
        return new AuthenticationService(userRepository, sessionRepository, refreshTokenRepository,
                roleService, jwtTokenService, passwordEncoder, properties);
    }

    /**
     * Configure the Internal DB IDP adapter as the IdpAdapter implementation.
     *
     * @param authenticationService the authentication service
     * @param userManagementService the user management service
     * @param roleService the role service
     * @param sessionRepository the session repository
     * @param jwtTokenService the JWT token service
     * @return the IDP adapter bean
     */
    @Bean
    @ConditionalOnMissingBean(IdpAdapter.class)
    public IdpAdapter internalDbIdpAdapter(AuthenticationService authenticationService,
                                           UserManagementService userManagementService,
                                           RoleService roleService,
                                           SessionRepository sessionRepository,
                                           JwtTokenService jwtTokenService) {
        return new InternalDbIdpAdapter(authenticationService, userManagementService,
                roleService, sessionRepository, jwtTokenService);
    }
}
