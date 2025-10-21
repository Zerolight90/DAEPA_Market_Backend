package com.daepamarket.daepa_market_backend.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@MapperScan(basePackages = {"com.daepamarket.daepa_market_backend.mapper"})

public class DBconfig {

    @Bean
    public SqlSessionFactory getFactiry(DataSource ds) throws Exception {
        //SqlSessionFactory를 생성하는 객체
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds); //DB연결

        //mapper폴더의 xml파일을 읽어온다.
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath:mapper/**/*.xml"));
        return factoryBean.getObject();

    }

    @Bean
    public SqlSessionTemplate getTemplate(SqlSessionFactory factory) {
        return new SqlSessionTemplate(factory);
    }
}
