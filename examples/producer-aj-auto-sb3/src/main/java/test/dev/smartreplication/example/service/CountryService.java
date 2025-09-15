package test.dev.smartreplication.example.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.smartreplication.example.entity.Country;
import test.dev.smartreplication.example.repository.CountryRepository;
import test.dev.smartreplication.example.type.CountryType;
import test.dev.smartreplication.example.utils.Utils;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Service
public class CountryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CountryRepository countryRepository;


    public CountryService(JdbcTemplate jdbcTemplate, CountryRepository countryRepository) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.countryRepository = countryRepository;
    }

    @Transactional
    public int save(String name, int population) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        final int nextval = jdbcTemplate.queryForObject("select nextval('users_id_sec')", Collections.emptyMap(), Integer.class);
        // задаем ключ партиционирования в kafka
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(nextval));
        params.addValue("id", nextval);
        params.addValue("name", name);
        params.addValue("description", name);
        params.addValue("population", population);
        params.addValue("country_type", "FULL_RECOGNIZED");
        params.addValue("amount", new BigDecimal("98765432.00"));
        final int updated = jdbcTemplate.update("insert into country (id, name, description, population, country_type, amount) " +
                        "values (:id, :name, :description, :population, :country_type::my_country_type, :amount)",
                params);
        return updated;
    }

    @Transactional
    public int save(int id, String name, int population) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        // id автоматически будет использоваться в качестве ключа партиционирования в kafka, см реализацию ChangeKeySupplier
        params.addValue("id", id);
        params.addValue("name", name);
        params.addValue("description", name);
        params.addValue("population", population);
        params.addValue("country_type", "FULL_RECOGNIZED");
        params.addValue("amount", new BigDecimal("12345678.00"));
        final int updated = jdbcTemplate.update("insert into country (id, name, description, population, country_type, amount) " +
                        "values (:id, :name, :description, :population, :country_type::my_country_type, :amount)",
                params);
        return updated;
    }

    @Transactional
    public int update(int id, int population) {
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(id));
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("population", population);
        final int updated = jdbcTemplate.update("update country set population=:population where id=:id", params);
        return updated;
    }

    @Transactional
    public int updateCustomTypeColumn(int id, String countryType) {
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(id));
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("country_type", countryType);
        final int updated = jdbcTemplate.update("update country set country_type=:country_type::my_country_type where id=:id", params);
        return updated;
    }

    @Transactional
    public int upsert(int id, String name, int population) {
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(id));
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("name", name);
        params.addValue("description", name);
        params.addValue("population", population);
        params.addValue("country_type", "FULL_RECOGNIZED");
        final int updated = jdbcTemplate.update("insert into country (id, name, population, country_type)" +
            " values (:id, :name, :population, :country_type::my_country_type)" +
                " on conflict (id) do update set name =:name, population=:population", params);
        return updated;
    }

    @Transactional
    public int multiUpdate(int population, List<Integer> ids) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("population", population);
        params.addValue("ids", ids);
        final int updated = jdbcTemplate.update("update country set population=:population where id IN (:ids)", params);
        return updated;
    }

    @Transactional
    public void multiUpdateWithResultSetProcessing(int population, List<Integer> ids) {
        log.info("multiUpdateWithResultSetProcessing start");
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("population", population);
        params.addValue("ids", ids);
        jdbcTemplate.query("update country set population=:population where id IN (:ids) returning id,name,population",
            params,
            (rs) -> {
                int pop = rs.getInt("population");
                log.info("Result set row {}", pop);
            });
        log.info("multiUpdateWithResultSetProcessing finish");
    }

    @Transactional
    public int delete() {
        final int updated = jdbcTemplate.update("delete from country", new MapSqlParameterSource());
        return updated;
    }

    @Transactional
    public Country createRandomCountryByJpa() {
        var country = createRandomCountry();
        log.info("createRandomCountryByJpa country.id={}", country.getId());
        var savedCountry = countryRepository.save(country);
        return savedCountry;
    }

    @Transactional
    public Country saveByJpa(Country country) {
        var countryEntity = countryRepository.save(country);
        log.info("saveByJpa country.id={}", countryEntity.getId());
        return countryEntity;
    }

    /**
     * Пример INSERT с RETURNING
     * ПЖ перезапишет набор полей returning на все поля.
     * Будет returning id,name,description,population,country_type,amount
     */
    @Transactional
    public Long createCountry() {
        var params = new MapSqlParameterSource();
        params.addValue("name", "newCountry_" + RandomStringUtils.random(10, true, false));
        params.addValue("description", "newDescription_" + RandomStringUtils.random(30, true, false));
        params.addValue("population", RandomUtils.nextInt());
        params.addValue("country_type", CountryType.FULL_RECOGNIZED.name());
        var country = jdbcTemplate.query("INSERT INTO country (name, description, population, country_type)" +
            " VALUES (:name, :description, :population, :country_type::my_country_type) RETURNING description", params, CountryService::map);
        return Optional.ofNullable(country).map(Country::getId).orElseThrow();
    }

    /**
     * Пример UPDATE с RETURNING
     * ПЖ перезапишет набор полей returning на поле id и изменяемые поля.
     * Будет returning id,description
     */
    @Transactional
    public Country updateCountry(Long id) {
        var params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("update_description", "_updated_" + LocalTime.now());
        var country = jdbcTemplate.query("UPDATE country SET description=CONCAT(description,:update_description) WHERE id=:id RETURNING id", params, CountryService::map);
        if (country == null || country.getDescription() == null) {
            throw new RuntimeException();
        }
        return country;
    }

    /**
     * Пример вставки нескольких записей c использованием multi-value insert SQL инструкции.
     * @param countryNumber количество записей для вставки.
     */
    @Transactional
    public List<Country> insertCountriesByMultiValueInsert(int countryNumber) {
        // Создаем список объектов для вставки.
        List<Country> countries = IntStream.range(0, countryNumber)
            .mapToObj(i -> createRandomCountry())
            .toList();

        // Подготавливаем multi-value insert.
        String sql = """
            INSERT INTO country(id, name, description, population, country_type, amount)
            VALUES
            """;

        // Формируем парамеры. Для каждой записи будет использоваться отдельный набор параметров.
        // id0, name0, ... параметры для записи 0
        // id1, name1, ... парметры для записи 1
        // для каждой вставляемой записи к тексту SQL запроса добавляется отдельный параметризованный кортеж
        // (:id0, :name0 ... ),
        // (:id1, :name1 ... ),

        MapSqlParameterSource params = new MapSqlParameterSource();
        for(int i=0; i < countryNumber; i++) {
            Country country = countries.get(i);

            // Добавляем именованные параметры
            params.addValue("id" + i, i);
            params.addValue("name" + i, country.getName());
            params.addValue("description" + i, country.getDescription());
            params.addValue("population" + i, country.getPopulation());
            params.addValue("country_type" + i, country.getCountryType().name());
            params.addValue("amount" + i, country.getAmount());

            // Добавляем параметризованный кортеж к тексту запроса
            sql += "(:id%d, :name%d, :description%d, :population%d, :country_type%d::my_country_type, :amount%d),\n"
                .formatted(i, i, i, i, i, i);
        }
        // Удаляем лишнюю запятую
        sql = sql.substring(0,  sql.length() - 2);

        // Добавляем RETURNING со всеми полями чтобы получить данные по фактически вставленным записям.
        sql += "\n RETURNING id, name, description, population, country_type, amount";

        log.info("Multi-value INSERT: {}", sql);

        // Выполняем multi-value insert.
        List<Country> countriesInserted = jdbcTemplate.query(sql, params, CountryService::mapRow);

        return countriesInserted;
    }

    /**
     * Пример вставки нескольких записей в режиме batch.
     * Такая операция НЕ ПЕРЕХВАТЫВАЕТСЯ Прикладным журналом и вектор изменений НЕ ФОРМИРУЕТСЯ.
     * Batch режим не поддерживается Прикладным журналом.
     *
     * @param countryNumber количество записей для вставки.
     */
    @Transactional
    public void insertCountriesByBatchInsert(int countryNumber) {
        // Создаем список объектов для вставки.
        List<Country> countries = IntStream.range(0, countryNumber)
            .mapToObj(i -> createRandomCountry())
            .toList();

        // Параметризованный INSERT который будем использовать для вставки нескольких записей
        String sql = """
            INSERT INTO country(id, name, description, population, country_type, amount)
            VALUES 
            (:id, :name, :description, :population, :country_type::my_country_type, :amount)
            """;

        // Подготавливаем массив параметров
        MapSqlParameterSource[] parameterSources = new MapSqlParameterSource[countryNumber];
        for (int i = 0; i < countryNumber; i++) {
            Country country = countries.get(i);
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", i);
            params.addValue("name", country.getName());
            params.addValue("description", country.getDescription());
            params.addValue("population", country.getPopulation());
            params.addValue("country_type", country.getCountryType().name());
            params.addValue("amount", country.getAmount());
            parameterSources[i] = params;
        }

        int[] updates = jdbcTemplate.batchUpdate(sql, parameterSources);
        // Так как библиотека прикладного журнала добавляет RETURNING то возвращаемые значения будут некорректными.
        // Чтобы возвращались корректные значения, необходимо отключать трансформацию batch SQL запросов
        // в конфигурации прикладного журнала test.dev.streaming.config.AdapterConfig.skipTransformQueryPredicates
        log.info("Number of affected rows for each batch entry: {}", Arrays.toString(updates));
    }

    /**
     * Пример использования инструкции SQL INSERT ... SELECT где источником вставляемых записей является запрос
     * SELECT. В данном пример вектор изменений формируется автоматически.
     * @return список скопированных объектов
     */
    @Transactional
    public List<Country> copyCountries() {
        // Копируем записи из таблицы country в эту же таблицу но с другими id.
        String sql = """
            INSERT INTO country(id, name, description, population, country_type, amount)
            SELECT id + 1000, name, description, population, country_type, amount FROM country
             RETURNING id, name, description, population, country_type, amount
            """;

        List<Country> countries = jdbcTemplate.query(sql, new MapSqlParameterSource(), CountryService::mapRow);
        return countries;
    }

    private static Country map(@NonNull ResultSet rs) {
        try {
            if (!rs.next()) {
                return null;
            }
            var country = new Country();
            country.setId(rs.getLong("id"));
            if (Utils.hasColumn(rs, "name")) {
                country.setName(rs.getString("name"));
            }
            if (Utils.hasColumn(rs, "description")) {
                country.setDescription(rs.getString("description"));
            }
            if (Utils.hasColumn(rs, "population")) {
                country.setPopulation(rs.getInt("population"));
            }
            if (Utils.hasColumn(rs, "country_type")) {
                country.setCountryType(CountryType.valueOf(rs.getString("country_type")));
            }
            return country;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Country mapRow(@NonNull ResultSet rs, int rowNum) {
        try {
            var country = new Country();
            country.setId(rs.getLong("id"));
            if (Utils.hasColumn(rs, "name")) {
                country.setName(rs.getString("name"));
            }
            if (Utils.hasColumn(rs, "description")) {
                country.setDescription(rs.getString("description"));
            }
            if (Utils.hasColumn(rs, "population")) {
                country.setPopulation(rs.getInt("population"));
            }
            if (Utils.hasColumn(rs, "country_type")) {
                country.setCountryType(CountryType.valueOf(rs.getString("country_type")));
            }
            return country;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Country createRandomCountry() {
        var country = new Country();
        country.setName("newCountry_" + RandomStringUtils.random(10, true, false));
        country.setDescription("newDescription_" + RandomStringUtils.random(30, true, false));
        country.setPopulation(RandomUtils.nextInt());
        country.setCountryType(CountryType.FULL_RECOGNIZED);
        country.setAmount(new BigDecimal(RandomUtils.nextLong()));
        return country;
    }

}
