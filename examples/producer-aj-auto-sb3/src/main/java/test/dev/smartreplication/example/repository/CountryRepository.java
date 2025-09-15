package test.dev.smartreplication.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import test.dev.smartreplication.example.entity.Country;

public interface CountryRepository extends JpaRepository<Country, Long> {
}
