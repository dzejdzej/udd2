package com.crkomi.udd2.rest.mvc;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.lucene.document.Document;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.crkomi.udd2.entities.Account;
import com.crkomi.udd2.entities.Analyzer;
import com.crkomi.udd2.entities.Benchmark;
import com.crkomi.udd2.entities.DocumentPath;
import com.crkomi.udd2.entities.InstalledBenchmarkImporter;
import com.crkomi.udd2.entities.QueryAndRelevantDocuments;
import com.crkomi.udd2.entities.RelevantDocument;
import com.crkomi.udd2.repositories.DocPathRepo;
import com.crkomi.udd2.services.AccountService;
import com.crkomi.udd2.services.AnalyzerService;
import com.crkomi.udd2.services.BenchmarkService;
import com.crkomi.udd2.services.InstalledBenchmarkImporterService;
import com.crkomi.udd2.services.QueryAndRelevantDocumentsService;
import com.crkomi.udd2.tools.indexer.UDDIndexer;

import benchmark.importer.core.contract.BenchmarkImporter;
import benchmark.importer.core.model.ImportedBenchmarkModel;
import benchmark.importer.core.model.ImportedQuery;

@Controller
@RequestMapping("/rest/benchmark-importer")
public class BenchmarkImporterController {

	/***
	 * 
	 * @Entity BenchmarkImporter
	 * 
	 *         POST installNewBenchmarkImporter(MUltiPart part) { String name =
	 *         part.getParam("fileName"); Jar. ...
	 * 
	 *         ubacimo JAR na putanju na disku ubacimo novi entiotet tipa
	 *         BemnchmarkImporter u tabelu...novoi red
	 * 
	 * 
	 * 
	 *         }
	 * 
	 * 
	 */

	///////////////////
	// Prosiriti benchmark controller
	// importBenchmarks POST
	// NAVODI tipBenchMarkImportera koga treba koristiti

	/************
	 * 
	 * na osnovu imena benchmarkImportera ,nadjemo ga iz baze BenchMarkImporter
	 * importer = benchmarkIMporterRespotiroty.findbyName(name); CLassloder load =
	 * ucitamo JAR fajl sa puitanje importer.getPath();\
	 * 
	 * Izvlacimo klasu BenchImporter iz jar-a BenchmarkImporter (interjfejs) imp =
	 * loader.findByClassName("BanchmarkImporter");
	 * 
	 * importer.validate() importer.import()
	 *
	 * 
	 */

	@Autowired
	ServletContext servletContext;

	@Autowired
	private BenchmarkService benchmarkService;

	
	@Autowired
	private QueryAndRelevantDocumentsService queryAndRelevantDocumentsService;
	
	@Autowired
	private InstalledBenchmarkImporterService installedBenchmarkImporterService;

	@Autowired
	private AnalyzerService analyzerService;

	@Autowired
	private AccountService accountService;

	@RequestMapping(value = "/newBenchmarkImporter", method = RequestMethod.POST)
	// @PreAuthorize("hasRole('User','Admin')")
	public ResponseEntity<?> uploadBenchmarkImporter(@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam("formDataJson") String formDataJson, @RequestParam("name") String benchmarkImporterName,
			@RequestParam("description") String description)
			throws COSVisitorException, IllegalStateException, IOException {

		// save jar file
		String benchmarkImporterRealPath = servletContext.getRealPath("/benchmark-importers") + "/"
				+ benchmarkImporterName;
		String fileName = file.getOriginalFilename();
		File destDir = new File(benchmarkImporterRealPath);
		if (!destDir.exists())
			destDir.mkdir();
		File destFile = new File(destDir + "/" + fileName);
		file.transferTo(destFile);

		// save new analyzer info
		InstalledBenchmarkImporter newBenchmarkImporter = new InstalledBenchmarkImporter();
		newBenchmarkImporter.setName(benchmarkImporterName);
		newBenchmarkImporter.setDescription(description);
		newBenchmarkImporter.setPath(benchmarkImporterRealPath + "/" + fileName);

		installedBenchmarkImporterService.installBenchmarkImporter(newBenchmarkImporter);

		return new ResponseEntity<String>(HttpStatus.OK);
	}

	@RequestMapping(value = "/importBenchmark", method = RequestMethod.POST)
//	@PreAuthorize("hasRole('User','Admin')")
	public ResponseEntity<?> importBenchmarkFromZip(@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam("benchmarkId") Long benchmarkId) throws COSVisitorException, IllegalStateException,
			IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		// Object principal = SecurityContextHolder.getContext()
		// .getAuthentication().getPrincipal();
		 Account user = null;
		// if (principal instanceof UserDetails) {
		// UserDetails details = (UserDetails) principal;
		// user = accountService.findAccountByUsername(details.getUsername());
		// }
		//
		//
		// load all Benchmark analyzers...
		List<BenchmarkImporter> allInstalledBenchmarkImporters = loadAllBenchmarkImporters();

		// find the one that can import this package
		BenchmarkImporter foundImporter = findBenchmarkImporterThatCanHandleFile(file, allInstalledBenchmarkImporters);
		if (foundImporter == null) {
			// if this upload cannot be imported, the client has made a mistake...
			// return bad req
			return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
		}

		// find the benchmark that all of this data should be appened to
		Benchmark benchmark = benchmarkService.findBenchmark(benchmarkId);
		// extract Bechmark data from uploaded ZIP file

		String docPath = servletContext.getRealPath(File.separator + "docs") + File.separator + "test" + File.separator
				+ benchmark.getDirectoryName();
		ImportedBenchmarkModel benchmarkData = foundImporter.importBenchmark(docPath, file.getInputStream());

		///////////////////////////////////////////////////////////////////////////////////////////////////
		String indexDir  = processAndIndexBenchmarkData(benchmark, benchmarkData, user, docPath);
		
		benchmark.setIndexDir(indexDir);
		
	
		///////////////////////////////////////////////////////////////////////////////////////////////////
		
		// ADD query data and relevant document data
		Set<QueryAndRelevantDocuments> queryAndRelevantDocumentsList = benchmark.getQueryAndRelevantDocumentsList();

		
		
		// add all queries 
		for(ImportedQuery importedQuery : benchmarkData.getQueryAndRelevantDocumentsList()) {
			QueryAndRelevantDocuments queryAndRelevantDocuments = new QueryAndRelevantDocuments();
			queryAndRelevantDocuments.setBenchmark(benchmark);
			
			Set<RelevantDocument> relevantDocuments = new HashSet<RelevantDocument>();
			for(String doc_id : importedQuery.getRelevantDocumentsPath()) {
				RelevantDocument rd = new RelevantDocument();
				rd.setQueryAndRelevantDocuments(queryAndRelevantDocuments);
				rd.setUid(doc_id);
				relevantDocuments.add(rd);
			}
			
			
			queryAndRelevantDocuments.setRelevantDocuments(relevantDocuments);
			
			queryAndRelevantDocuments.setText(importedQuery.getText());
			queryAndRelevantDocuments.setTextSearchType(importedQuery.getSearchType());
			queryAndRelevantDocumentsService.createQueryAndRelevantDocuments(queryAndRelevantDocuments);
			
		
			
			
			queryAndRelevantDocumentsList.add(queryAndRelevantDocuments);
		}
		
		benchmark.setQueryAndRelevantDocumentsList(queryAndRelevantDocumentsList);
		
		benchmarkService.updateBenchmark(benchmark);

		return new ResponseEntity<String>(HttpStatus.OK);
	}


	@Autowired DocPathRepo documentPathRepo;
	
	
	private String processAndIndexBenchmarkData(Benchmark benchmark, ImportedBenchmarkModel benchmarkData, Account user, String docPath) throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		File docsDirAllDocs = new File(docPath);
		String indexDir = servletContext.getRealPath(File.separator + "indexes") + File.separator + System.currentTimeMillis();
		
		File indexDirFolder = new File(indexDir);
		indexDirFolder.mkdirs();
		
		Analyzer selectedAnalyzer = getAnalyzer(benchmark);
		org.apache.lucene.analysis.Analyzer luceneAnalyzer = getLuceneAnalyzer(selectedAnalyzer);
		UDDIndexer UDDIndexer = new UDDIndexer(indexDir);
		UDDIndexer.index(docsDirAllDocs, luceneAnalyzer, indexDir);

		// create new benchmark
		benchmark.setIndexDir(indexDir);

		Set<DocumentPath> allDocumentsPath = new HashSet<DocumentPath>();
		Class<?> klazz = Document.class;
		Document d = new Document();
		Document[] allDocs = UDDIndexer.getAllDocuments();
		for (int i = 0; i < allDocs.length; i++) {
			DocumentPath dp = new DocumentPath();
			Document doc = allDocs[i];
			dp.setPath(doc.get("location"));
			dp.setBenchmark(benchmark);
			documentPathRepo.save(dp);
			allDocumentsPath.add(dp);
		}
		benchmark.setAllDocumentsPath(allDocumentsPath);
		benchmark.setAnalyzerType(benchmark.getAnalyzerType());
		benchmark.setName(benchmark.getName());
		benchmark.setDirectoryName(benchmark.getDirectoryName());
		benchmark.setAnalyzerName(selectedAnalyzer.getName());
		//benchmark.setAccount(user);
		benchmarkService.updateBenchmark(benchmark);

		//Set<Benchmark> benchmarks = user.getBenchmarks();
		//benchmarks.add(benchmark);
		//user.setBenchmarks(benchmarks);
		//accountService.updateAccount(user);
		return indexDir;
	}

	private org.apache.lucene.analysis.Analyzer getLuceneAnalyzer(Analyzer analyzer) throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		URL[] urls = { new URL("jar:file:" + analyzer.getPath()+"!/") };
		@SuppressWarnings("resource")
		URLClassLoader cl = new URLClassLoader(urls,Thread.currentThread().getContextClassLoader());

		Class<org.apache.lucene.analysis.Analyzer> classToLoad = (Class<org.apache.lucene.analysis.Analyzer>) cl.loadClass(analyzer.getName());
		return (org.apache.lucene.analysis.Analyzer) classToLoad.newInstance();	 
	}

	private Analyzer getAnalyzer(Benchmark benchmark) {
		List<Analyzer> analyzers = analyzerService.getAllAnalyzers();
		for(Analyzer analyzer : analyzers) {
			if(analyzer.getName().equals(benchmark.getAnalyzerName())) {
				return analyzer;
			}
		}
		return null;
	}

	private List<BenchmarkImporter> loadAllBenchmarkImporters()
			throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		List<InstalledBenchmarkImporter> installedBenchmarkImporters = installedBenchmarkImporterService
				.getAllBenchmarkImporters();
		List<BenchmarkImporter> loadedImporters = new ArrayList<>();
		for (InstalledBenchmarkImporter installedImporter : installedBenchmarkImporters) {
			loadedImporters.add(loadBenchmarkImporter(installedImporter));
		}
		return loadedImporters;
	}

	private BenchmarkImporter loadBenchmarkImporter(InstalledBenchmarkImporter installedImporter)
			throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// dynamically load selected benchmark importer

		// analyzerService.findAnalyzer(benchmark.getAnalyzerType());
		URL[] urls = { new URL("jar:file:" + installedImporter.getPath() + "!/") };
		@SuppressWarnings("resource")
		URLClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

		@SuppressWarnings("unchecked")
		Class<BenchmarkImporter> classToLoad = (Class<BenchmarkImporter>) cl.loadClass(installedImporter.getName());
		return (BenchmarkImporter) classToLoad.newInstance();
	}

	private BenchmarkImporter findBenchmarkImporterThatCanHandleFile(MultipartFile file,
			List<BenchmarkImporter> allInstalledBenchmarkImporters) throws IOException {

		for (BenchmarkImporter importer : allInstalledBenchmarkImporters) {
			if (importer.canImport(file.getInputStream())) {
				return importer;
			}
		}

		return null;

	}

}
