package com.crkomi.udd2.tools.util;

import java.util.ArrayList;
import java.util.List;

import com.crkomi.udd2.entities.InstalledBenchmarkImporter;

public class BenchmarkImporterList {

	private List<InstalledBenchmarkImporter> analyzers = new ArrayList<InstalledBenchmarkImporter>();

	public List<InstalledBenchmarkImporter> getAnalyzers() {
		return analyzers;
	}

	public void setAnalyzers(List<InstalledBenchmarkImporter> analyzers) {
		this.analyzers = analyzers;
	}
	
}
