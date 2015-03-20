package sk.ondrejhirjak.db;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.flywaydb.core.Flyway;
import sk.ondrejhirjak.db.dao.Dao;
import sk.ondrejhirjak.server.Configuration;
import sk.ondrejhirjak.module.ServerModule;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;


public class DbContext implements ServerModule {

    private static final Logger LOGGER = Logger.getLogger(DbContext.class);

    private static final String DEFAULT_RESOURCE = "db/mybatis.xml";

    private static final String DB_SCRIPTS_PATH = "classpath:db/version/";

    private String resource;

    private Set<Dao<?>> daos;


    public DbContext(String resource) {
        this.resource = resource;
    }


    public DbContext() {
        this(DEFAULT_RESOURCE);
    }


    @Override
    public void init(Configuration configuration) {
        InputStream inputStream = null;
        try {
            inputStream = Resources.getResourceAsStream(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.info("Connecting to database url: '" + configuration.dbUrl + "' as user: '" + configuration.dbUser + "'");

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, buildDbProperties(configuration));

        migrateDb(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), configuration);

        if (daos != null) {
            for (Dao<?> dao : daos) {
                dao.init(sqlSessionFactory);
            }
        }
    }


    @Override
    public void start() {

    }


    @Override
    public void stop() {

    }


    private Properties buildDbProperties(Configuration configuration) {
        Properties dbProperties = new Properties();

        dbProperties.setProperty("db.url", configuration.dbUrl);
        dbProperties.setProperty("db.username", configuration.dbUser);
        dbProperties.setProperty("db.password", configuration.dbPass);

        return dbProperties;
    }


    private void migrateDb(DataSource dataSource, Configuration configuration) {
        LOGGER.debug("dbAutoMigration: " + configuration.dbAutoMigration);

        if (configuration.dbAutoMigration) {
            Flyway flyway = new Flyway();

            flyway.setDataSource(dataSource);

            flyway.migrate();
        }
    }


    public void setDaos(Set<Dao<?>> daos) {
        this.daos = daos;
    }
}