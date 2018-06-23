package com.crkomi.udd2.rest.mvc;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.crkomi.udd2.entities.Analyzer;
import com.crkomi.udd2.services.AnalyzerService;
import com.crkomi.udd2.services.BenchmarkService;
import com.crkomi.udd2.tools.util.BenchmarkList;


@Controller
@RequestMapping("/rest/benchmark-importer")
public class BenchmarkImporterController {
	
	/***
	 * 
	 * @Entity
	 * BenchmarkImporter
	 * 
	 * POST
	 * installNewBenchmarkImporter(MUltiPart part) {
	 * 		String name = part.getParam("fileName");
	 * 		Jar. ... 
	 * 
	 * 		ubacimo JAR na putanju na disku
	 * ubacimo novi entiotet tipa BemnchmarkImporter u tabelu...novoi red
	 * 
	 * 
	 * 
	 * }
	 * 
	 * 
	 */
	
	///////////////////
	// Prosiriti benchmark controller
	// importBenchmarks POST
	//  NAVODI tipBenchMarkImportera koga treba koristiti
	
	/************
	 * 
	 *   na osnovu imena benchmarkImportera ,nadjemo ga iz baze
	 *   BenchMarkImporter importer = benchmarkIMporterRespotiroty.findbyName(name);
	 *   CLassloder load = ucitamo JAR fajl sa puitanje  importer.getPath();\
	 *   
	 *   Izvlacimo klasu BenchImporter iz jar-a
	 *   BenchmarkImporter (interjfejs) imp = loader.findByClassName("BanchmarkImporter");
	 *   
	 *   importer.validate()
	 *   importer.import()
	 *
	 * 
	 */
	
	 	@Autowired
	    ServletContext servletContext;
		
	    @Autowired
		private BenchmarkService benchmarkService;
	    
	    @Autowired
		private AnalyzerService analyzerService;
	    
	    @RequestMapping(value = "/newBenchmarkImporter", method = RequestMethod.POST)
		@PreAuthorize("hasRole('User','Admin')")
		public ResponseEntity<?> uploadBenchmarkImporter(
				@RequestParam(value = "file", required = false) MultipartFile file, @RequestParam("formDataJson") String formDataJson,
				 @RequestParam("name") String analyzerName, @RequestParam("description") String description) throws COSVisitorException, IllegalStateException, IOException {
					
			//save jar file
			String analyzerRealPath = servletContext.getRealPath("/analyzers") + "/" + analyzerName;
			String fileName = file.getOriginalFilename();
			File destDir = new File(analyzerRealPath);
			if(!destDir.exists())
				destDir.mkdir();
			File destFile = new File(destDir + "/" + fileName);
			file.transferTo(destFile);		
					
			//save new analyzer info
			Analyzer newAnalyzer = new Analyzer();
			newAnalyzer.setName(analyzerName);
			newAnalyzer.setDescription(description);
			newAnalyzer.setPath(analyzerRealPath + "/" + fileName);
			
			analyzerService.createAnalyzer(newAnalyzer);
			  	  
			return new ResponseEntity<String>(HttpStatus.OK);
		}
		 
		@RequestMapping(value="/importBenchmark",method = RequestMethod.POST)
		@PreAuthorize("hasRole('User','Admin')")
	    public ResponseEntity<BenchmarkList> importBenchmarkFromZip(	@RequestParam(value = "file", required = false) MultipartFile file, @RequestParam("formDataJson") String formDataJson,
				 @RequestParam("name") String analyzerName, @RequestParam("description") String description) throws COSVisitorException, IllegalStateException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
			
		//	Benchmark benchmarkdb = benchmarkService.findBenchmark(benchmark.getBenchmark_id());
			
			//dynamically load selected analyzer
			//core.models.entities.Analyzer selectedAnalyzer = analyzerService.findAnalyzer(benchmark.getAnalyzerType());
			/*
			URL[] urls = { new URL("jar:file:" + selectedAnalyzer.getPath()+"!/") };
			@SuppressWarnings("resource")
			URLClassLoader cl = new URLClassLoader(urls,Thread.currentThread().getContextClassLoader());		

			@SuppressWarnings("unchecked")
			Class<org.apache.lucene.analysis.Analyzer> classToLoad = (Class<org.apache.lucene.analysis.Analyzer>) cl.loadClass(selectedAnalyzer.getName());
			org.apache.lucene.analysis.Analyzer analyzer =  (org.apache.lucene.analysis.Analyzer) classToLoad.newInstance();	
			*/
			return null;
		}

}
