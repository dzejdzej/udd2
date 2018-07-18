package com.crkomi.udd2.rest.mvc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.crkomi.udd2.repositories.InstalledBenchmarkImporterRepo;
import com.crkomi.udd2.services.AccountService;
import com.crkomi.udd2.services.AnalyzerService;
import com.crkomi.udd2.services.BenchmarkService;
import com.crkomi.udd2.services.InstalledBenchmarkImporterService;
import com.crkomi.udd2.services.QueryAndRelevantDocumentsService;
import com.crkomi.udd2.tools.indexer.UDDIndexer;
import com.crkomi.udd2.tools.models.AnalyzerModel;
import com.crkomi.udd2.tools.util.AnalyzerList;
import com.crkomi.udd2.tools.util.BenchmarkImporterList;
import com.crkomi.udd2.tools.util.BenchmarkList;

import benchmark.importer.core.contract.BenchmarkImporter;
import benchmark.importer.core.model.ImportedBenchmarkModel;
import benchmark.importer.core.model.ImportedQuery;

@Controller
@RequestMapping("/LuceneAnalyzerTester/rest/benchmark-importer")
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

		
		File dir =  new File(servletContext.getRealPath("/benchmark-importers"));

		if(!dir.exists()) {
			dir.mkdir();
		}
		
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
			///////queryAndRelevantDocumentsService.createQueryAndRelevantDocuments(queryAndRelevantDocuments);
			
			
			
			try(FileWriter fw = new FileWriter("skripta.txt", true);
				    BufferedWriter bw = new BufferedWriter(fw);
				    PrintWriter writer = new PrintWriter(bw))
				{
				//query_and_relevant_documents	  text, text_search_type, benchmark_id
				//INSERT INTO TABLE_NAME (column1, column2, column3, ...) VALUES (value1, value2, value3, ...);
				writer.println("INSERT INTO Query_and_relevant_documents (text, text_search_type, benchmark_id) "
						+ " VALUES ('" + queryAndRelevantDocuments.getText() + "', '" + queryAndRelevantDocuments.getTextSearchType() + "', " + queryAndRelevantDocuments.getBenchmark().getBenchmark_id() + ");");
				
				//INSERT INTO tbl (auto,text) VALUES(NULL,'text');
				//INSERT INTO tbl2 (id,text) VALUES(LAST_INSERT_ID(),'text');
				
				writer.println("set @lastId = last_insert_id();");
				
				for(String rd : importedQuery.getRelevantDocumentsPath()) {
					writer.println("INSERT INTO Relevant_document (uid, query_and_relevant_documents_id) "
						+ " VALUES ('" + rd + "', " + "@lastId);");
				
				}
				
				} catch (IOException e) {
				    e.printStackTrace();
				}
			
			
		
			
			
			//queryAndRelevantDocumentsList.add(queryAndRelevantDocuments);
		}
		
		benchmark.setQueryAndRelevantDocumentsList(queryAndRelevantDocumentsList);
		
		benchmarkService.updateBenchmark(benchmark);

		
		
		final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
		final String DB_URL = "jdbc:mysql://localhost:3306/udd";

		//  Database credentials
		final String USER = "root";
		final String PASS = "root";
		
		try {
		      String line;
		      Process p = Runtime.getRuntime().exec
		        ("psql -U root -P root -d udd -h localhost -f scripta.txt");
		      BufferedReader input =
		        new BufferedReader
		          (new InputStreamReader(p.getInputStream()));
		      while ((line = input.readLine()) != null) {
		        System.out.println(line);
		      }
		      input.close();
		    }
		    catch (Exception err) {
		      err.printStackTrace();
		    }
		
		
		
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
			//documentPathRepo.save(dp);
			
			try(FileWriter fw = new FileWriter("skripta.txt", true);
				    BufferedWriter bw = new BufferedWriter(fw);
				    PrintWriter writer = new PrintWriter(bw))
				{
				//document_path     path, benchmark_id
				//INSERT INTO TABLE_NAME (column1, column2, column3, ...) VALUES (value1, value2, value3, ...);
				
				
				writer.println("INSERT INTO Document_Path (path, benchmark_id) VALUES ('" + dp.getPath() + "', " + dp.getBenchmark().getBenchmark_id() + ");");
				writer.println("set @dpId = last_insert_id();");
				
				} catch (IOException e) {
					e.printStackTrace();
				    //exception handling left as an exercise for the reader
				}
			
			
			//allDocumentsPath.add(dp);
		}
		benchmark.setAllDocumentsPath(allDocumentsPath);
		benchmark.setAnalyzerType(benchmark.getAnalyzerType());
		benchmark.setName(benchmark.getName());
		benchmark.setDirectoryName(benchmark.getDirectoryName());
		benchmark.setAnalyzerName(selectedAnalyzer.getName());
		//benchmark.setAccount(user);
		/////////////////////////////////////////////////////////////////
		benchmarkService.updateBenchmark(benchmark);
	/*
		
		PrintWriter writer =  null;
		try {
			writer = new PrintWriter("skripta.txt", "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//benchmark 	  analyzer_name, analyzer_type, directory_name, index_dir
		//UPDATE table_name	SET column1 = value1, column2 = value2, ...	WHERE condition;
		/* writer.println("UPDATE Benchmark SET "
				+ "analyzer_name='" + benchmark.getAnalyzerName() 
				+ "', analyzer_type=" + benchmark.getAnalyzerType() 
				+ ", directory_name='" + benchmark.getDirectoryName() 
				+ "', index_dir='" + benchmark.getIndexDir() 
		+ "' WHERE benchmark_id=" + benchmark.getBenchmark_id() + ";");
		writer.close();
		
		*/
		
		/////////////////////////////////////////////////////////////////
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
	
	@RequestMapping(value="/getAll",method = RequestMethod.GET)
	@PreAuthorize("hasRole('User','Admin')")
    public ResponseEntity<BenchmarkImporterList> getAllAnalyzers() {
		
		List<InstalledBenchmarkImporter> analyzers = installedBenchmarkImporterService.getAllBenchmarkImporters();

		BenchmarkImporterList analyzerList = new BenchmarkImporterList();
		analyzerList.setAnalyzers(analyzers);
		
		return new ResponseEntity<BenchmarkImporterList>(analyzerList,HttpStatus.OK);
		
	}
	
	
	@Autowired InstalledBenchmarkImporterRepo installedBenchmarkImporterRepo;
	
	@RequestMapping(value="/remove",method = RequestMethod.POST)
	@PreAuthorize("hasRole('User','Admin')")
    public ResponseEntity<String> removeAnalyzer(@RequestBody Long benchmarkId) {
		
		InstalledBenchmarkImporter importer = installedBenchmarkImporterRepo.findById(benchmarkId).get();
		String analyzerRealPath = servletContext.getRealPath(File.separator + "benchmark-importers") + File.separator + importer.getName();
		File file = new File(analyzerRealPath);
	    if (file.exists()) {
		     File[] files = file.listFiles();
		     for (int i = 0; i < files.length; i++) {		          
		          files[i].delete();
		     }
		}
        file.delete();
        installedBenchmarkImporterRepo.delete(importer);
		
		return new ResponseEntity<String>("Benchmark importer has been removed successfully", HttpStatus.OK);
	}
	

}
