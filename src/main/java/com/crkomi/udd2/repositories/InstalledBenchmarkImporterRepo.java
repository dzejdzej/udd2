package com.crkomi.udd2.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.crkomi.udd2.entities.InstalledBenchmarkImporter;

@Repository
public interface InstalledBenchmarkImporterRepo extends CrudRepository<InstalledBenchmarkImporter, Long> {
	
	
}
