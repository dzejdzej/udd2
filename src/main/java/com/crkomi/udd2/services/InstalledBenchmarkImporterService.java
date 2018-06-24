package com.crkomi.udd2.services;

import java.util.List;

import com.crkomi.udd2.entities.InstalledBenchmarkImporter;

public interface InstalledBenchmarkImporterService {
	
	public InstalledBenchmarkImporter installBenchmarkImporter(InstalledBenchmarkImporter data);
    public InstalledBenchmarkImporter removeAnalyzer(InstalledBenchmarkImporter data);
    public List<InstalledBenchmarkImporter> getAllBenchmarkImporters();
}
