//package com.dlab.rna.upload.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//
//import javax.sql.DataSource;
//
//@Configuration
//public class VectorDbConfig {
//    @Value("${pgvector.url}")
//    private String url;
//    @Value("${pgvector.username}")
//    private String username;
//    @Value("${pgvector.password}")
//    private String password;
//    @Value("${pgvector.driver-class-name}")
//    private String driverClassName;
//
//    @Bean
//    public DataSource vectorDataSource() {
//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        ds.setDriverClassName(driverClassName);
//        ds.setUrl(url);
//        ds.setUsername(username);
//        ds.setPassword(password);
//        return ds;
//    }
//
//    @Bean
//    public JdbcTemplate vectorJdbcTemplate(DataSource vectorDataSource) {
//        return new JdbcTemplate(vectorDataSource);
//    }
//}