package com.crkomi.udd2.services.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.crkomi.udd2.entities.InstalledBenchmarkImporter;
import com.crkomi.udd2.repositories.InstalledBenchmarkImporterRepo;
import com.crkomi.udd2.services.InstalledBenchmarkImporterService;


@Service
public class InstalledBenchmarkImporterServiceImpl implements InstalledBenchmarkImporterService {

	@Autowired 
	InstalledBenchmarkImporterRepo installedBenchmarkImporterRepo;




	public InstalledBenchmarkImporter removeAnalyzer(InstalledBenchmarkImporter data) {
		// TODO Auto-generated method stub
		installedBenchmarkImporterRepo.deleteById(data.getId());
		return data;
	}

	public List<InstalledBenchmarkImporter> getAllBenchmarkImporters() {
		List<InstalledBenchmarkImporter> asList = new ArrayList<>();
		// JAVA 8 magic via stackoverflow
		// iterator + stream method
		// https://stackoverflow.com/questions/6416706/easy-way-to-change-iterable-into-collection
		Iterator<InstalledBenchmarkImporter> iterator = installedBenchmarkImporterRepo.findAll().iterator();
		iterator.forEachRemaining(asList::add);
		return asList;
	}

	public InstalledBenchmarkImporter installBenchmarkImporter(InstalledBenchmarkImporter data) {
		return installedBenchmarkImporterRepo.save(data);
	}
	
	
	
	
}
