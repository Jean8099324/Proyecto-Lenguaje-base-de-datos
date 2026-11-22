package com.rescatevial.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Propaga el usuario web a la sesión Oracle usada por cada operación JDBC. */
@Configuration
public class AuditoriaDataSourceConfig {

    @Bean
    static BeanPostProcessor auditoriaDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource dataSource
                        && !(bean instanceof ClientIdentifierDataSource)) {
                    return new ClientIdentifierDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    private static final class ClientIdentifierDataSource extends DelegatingDataSource {
        private ClientIdentifierDataSource(DataSource delegate) {
            super(delegate);
        }

        @Override
        public Connection getConnection() throws java.sql.SQLException {
            return decorate(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws java.sql.SQLException {
            return decorate(super.getConnection(username, password));
        }

        private Connection decorate(Connection connection) throws java.sql.SQLException {
            String identifier = authenticatedUsername();
            setIdentifier(connection, identifier);
            InvocationHandler handler = new ConnectionHandler(connection);
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
        }

        private static String authenticatedUsername() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            String username = authentication.getName();
            return username == null || username.isBlank() ? null : username.substring(0, Math.min(100, username.length()));
        }

        private static void setIdentifier(Connection connection, String identifier) throws java.sql.SQLException {
            try (CallableStatement statement = connection.prepareCall("BEGIN DBMS_SESSION.SET_IDENTIFIER(?); END;")) {
                statement.setString(1, identifier);
                statement.execute();
            }
        }

        private static void clearIdentifier(Connection connection) {
            try (CallableStatement statement = connection.prepareCall("BEGIN DBMS_SESSION.CLEAR_IDENTIFIER; END;")) {
                statement.execute();
            } catch (Exception ignored) {
                // No ocultar la excepción original de la operación JDBC.
            }
        }

        private static final class ConnectionHandler implements InvocationHandler {
            private final Connection delegate;
            private boolean closed;

            private ConnectionHandler(Connection delegate) {
                this.delegate = delegate;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("close".equals(method.getName()) && !closed) {
                    closed = true;
                    clearIdentifier(delegate);
                }
                try {
                    return method.invoke(delegate, args);
                } catch (java.lang.reflect.InvocationTargetException exception) {
                    throw exception.getCause();
                }
            }

        }
    }
}
