package com.vgrazi.demo.dbunit;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;

@Configuration
public class AppConfig
{
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static final String SYBASE_DRIVER_CLASSNAME = "net.sourceforge.jtds.jdbc.Driver";
    private static final String CONNECTION_URL_TEMPLATE = "jdbc:jtds:sybase://%s:%s/%s";
    private final Environment env;

    @Inject
    public AppConfig(Environment env)
    {
        this.env = env;
    }

    @Bean
    public DataSource propstoreDataSource()
    {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(SYBASE_DRIVER_CLASSNAME);
        final String server = env.getRequiredProperty("db.databaseServer");
        final String port = env.getRequiredProperty("db.portNumber");
        final String databaseName = env.getRequiredProperty("db.databaseName");
        String url = String.format(CONNECTION_URL_TEMPLATE, server, port, databaseName);
        dataSource.setUrl(url);
        final String username = env.getRequiredProperty("db.username");
        dataSource.setUsername(username);
        dataSource.setPassword(env.getRequiredProperty("db.password"));
        final String environment = env.getRequiredProperty("env");
        logger.info("\nDataSource Info: [{}] {}:{}/{} [{}]", environment,
                server, port, databaseName, username);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate()
    {
        final JdbcTemplate template = new JdbcTemplate();
        template.setDataSource(propstoreDataSource());
        return template;
    }

}
