/*
 * Copyright (c) 2014, Jonas Ibn-Salem <ibnsalem@molgen.mpg.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package genomicregions;

import io.TabFileParser;
import io.TabFileParserTest;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import ontologizer.go.Term;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import phenotypeontology.PhenotypeData;
import toyexampledata.ExampleData;

/**
 *
 * @author Jonas Ibn-Salem <ibnsalem@molgen.mpg.de>
 */
public class AnnotateCNVsTest {
    
    // declare some variables to be knwon in this test
    private static GenomicSet<CNV> cnvs; 
    private static GenomicSet<CNV> exampleCNVs; 
    private static GenomicSet<GenomicElement> domains;
    private static GenomicSet<GenomicElement> boundaries;
    //private static GenomicElementSet<GenGenomicSet  public AnnotateCNVsTest() {
    private static GenomicSet<Gene> genes;
    
    private static ExampleData exampleData;
    
    @BeforeClass
    public static void setUpClass() throws IOException {

        // read sample CNVs:
        String cnvPath = TabFileParserTest.class.getResource("/sample_CNV_chr22.tab").getPath();
        TabFileParser cnvParser = new TabFileParser(cnvPath);
        cnvs = cnvParser.parseCNV();

        // read sample boundary elements
        String boundaryPath = TabFileParserTest.class.getResource("/sample_boundary_chr22.tab.addOne").getPath();
        TabFileParser boundaryParser = new TabFileParser(boundaryPath);
        boundaries = boundaryParser.parse();
                
        // read sample genes elements
        String genePath = TabFileParserTest.class.getResource("/sample_genes_chr22.tab").getPath();
        TabFileParser geneParser = new TabFileParser(genePath);
        genes = geneParser.parseGene();

        // parse toy example data set
        exampleData = new ExampleData();
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of boundaryOverlap method, of class AnnotateCNVs.
     */
    @Test
    public void testBoundaryOverlap() {
        System.out.println("boundaryOverlap");
        
        //System.out.println(inversions);
        //System.out.println(boundaries);
        
        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);
        
        // check if all boundaries are read.
        // The sample file containes 30 elements
        assertEquals(boundaries.size(), 30);
        
        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);
        
        // count number of inversions that overlap any boundary
        Integer cnt = 0;
        for (CNV c : cnvs.values()){
            if (! c.getBoundaryOverlap().isEmpty()){
                cnt++;
            }
        }
        // 34 of the sample CNVs should overlap completely at leaste one boundary, 
        // as calulated with the python script 'filter_overlap.py' from the barrier project.
        Integer expCnt = 34;
        assertEquals(expCnt, cnt);


    }

    /**
     * Test of annotateOverlappedGenes method, of class AnnotateCNVs.
     */
    @Test
    public void testGeneOverlap() {
        System.out.println("geneOverlap");
        AnnotateCNVs.annotateOverlappedGenes(cnvs, genes);
        
    }

    /**
     * Test of defineAdjacentRegionsByDomains method, of class AnnotateCNVs.
     */
    @Test
    public void testDefineAdjacentRegionsByDomains() {
        System.out.println("defineAdjacentRegionsByDomains");
        
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        
        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        
        CNV cnv1 = cnvs.get("cnv1");
        GenomicElement expLeftRegion = new GenomicElement("chr1", 0, 9, "leftAdjacentRegion");
        GenomicElement expRightRegion = new GenomicElement("chr1", 19, 37, "rightAdjacentRegion");
        System.out.println("left exp: " + expLeftRegion);
        System.out.println("left act: " + cnv1.getLeftAdjacentRegion());
        System.out.println("equals? " + expLeftRegion.equals(cnv1.getLeftAdjacentRegion()));
        assertTrue(expLeftRegion.equals(cnv1.getLeftAdjacentRegion()));

        System.out.println("right exp: " + expRightRegion);
        System.out.println("right act: " + cnv1.getRightAdjacentRegion());
        System.out.println("equals? " + expRightRegion.equals(cnv1.getRightAdjacentRegion()));
        assertTrue(expRightRegion.equals(cnv1.getRightAdjacentRegion()));
    
        CNV cnv3 = cnvs.get("cnv3");
        GenomicElement expLeftRegionCnv3 = new GenomicElement("chr1", 0, 12, "leftAdjacentRegion");
        GenomicElement expRightRegionCnv3 = new GenomicElement("chr1", 19, 37, "rightAdjacentRegion");
        assertEquals(expLeftRegionCnv3, cnv3.getLeftAdjacentRegion());
        assertEquals(expRightRegionCnv3, cnv3.getRightAdjacentRegion());
        
    }


    /**
     * Test of phenogramScore method, of class AnnotateCNVs.
     */
    @Test
    public void testPhenogramScore() {
        
        System.out.println("phenogramScore");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        GenomicSet<Gene> exampleGenes = exampleData.getGenes();
        domains = exampleData.getDomains();
 
        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);        
        AnnotateCNVs.annotateOverlappedGenes(cnvs, exampleGenes);
        AnnotateCNVs.annotateAdjacentGenes(cnvs, exampleGenes);        
        AnnotateCNVs.phenogramScore(cnvs, phenotypeData);
        
        Double expCnv1PhenoScore = 0.29; // see table in {@link ExampleData} doc
        Double expCnv2PhenoScore = 1.68; // see table in {@link ExampleData} doc

        assertEquals(expCnv1PhenoScore, cnvs.get("cnv1").getOverlapPhenogramScore(), 0.01);
        assertEquals(expCnv2PhenoScore, cnvs.get("cnv2").getOverlapPhenogramScore(), 0.01);
        
        CNV cnv1 = cnvs.get("cnv1");
        Double leftScore = cnv1.getLeftAdjacentPhenogramScore();
        assertEquals(0.0, leftScore, 0.01);

        assertEquals(1.68, cnv1.getRightAdjacentPhenogramScore(), 0.01);


    }



    /**
     * Test of annotateTDBD method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateTDBD() throws IOException {
        System.out.println("annotateTDBD");
        exampleData = new ExampleData();
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        HashMap<Term,HashSet<String>> terms2genes = exampleData.getTargetTerm2targetGene();
          
        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);
        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        AnnotateCNVs.annotateOverlappedGenes(cnvs, genes);
        AnnotateCNVs.annotateAdjacentGenes(cnvs, genes);
        AnnotateCNVs.phenogramScore(cnvs, phenotypeData);
        AnnotateCNVs.annotateAdjacentEnhancers(cnvs, enhancer);    
 
        AnnotateCNVs.annotateTDBD(cnvs, terms2genes);
        
        assertEquals("TDBD", cnvs.get("cnv1").getEffectMechanism("TDBD"));
        assertEquals("GDE", cnvs.get("cnv2").getEffectMechanism("TDBD"));
        assertEquals("TDBD", cnvs.get("cnv3").getEffectMechanism("TDBD"));
        assertEquals("NoData", cnvs.get("cnv4").getEffectMechanism("TDBD"));
    }

    /**
     * Test of annotateTDBDjustByScore method, of class AnnotateCNVs.
     */
//    @Test
//    public void testAnnotateTDBDjustByScore() {
//        System.out.println("annotateTDBDjustByScore");
//        GenomicSet<CNV> inversions = exampleData.getCnvs();
//        GenomicSet<Gene> genes = exampleData.getGenes();
//        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
//        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
//        GenomicSet<GenomicElement> domains = exampleData.getDomains();
//        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
//
//          
//        AnnotateCNVs.defineAdjacentRegionsByDomains(inversions, domains);
//        AnnotateCNVs.boundaryOverlap(inversions, boundaries);
//        AnnotateCNVs.annotateOverlappedGenes(inversions, genes);
//        AnnotateCNVs.annotateAdjacentGenes(inversions, genes);        
//        AnnotateCNVs.phenogramScore(inversions, phenotypeData);
//
//        AnnotateCNVs.annotateTDBDjustByScore(inversions, enhancer);
//        
//        assertTrue(inversions.get("cnv1").isTDBD());
//        assertFalse(inversions.get("cnv2").isTDBD());
//        assertFalse(inversions.get("cnv3").isTDBD());
//        assertFalse(inversions.get("cnv4").isTDBD());
//    }


    /**
     * Test of defineOverlappedDomainRegions method, of class AnnotateCNVs.
     */
    @Test
    public void testDefineOverlappedDomainRegions() {
        System.out.println("defineOverlappedDomainRegions");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        
        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);        
        AnnotateCNVs.defineOverlappedDomainRegions(cnvs, domains);
        
        GenomicElement leftOverlappedCnv1 = new GenomicElement("chr1", 9, 12, "leftOverlapped");
        GenomicElement rightOverlappedCnv1 = new GenomicElement("chr1", 15, 19, "rightOverlapped");
        
        assertEquals(leftOverlappedCnv1, cnvs.get("cnv1").getLeftOverlappedDomainRegion());
        assertEquals(rightOverlappedCnv1, cnvs.get("cnv1").getRightOverlappedDomainRegion());
        
        // cnv4 left border overlaps domain
        GenomicElement leftOverlappedCnv4 = new GenomicElement("chr1", 14, 14, "leftOverlapped");
        assertEquals(leftOverlappedCnv4, cnvs.get("cnv4").getLeftOverlappedDomainRegion());
    }

    /**
     * Test of tandemDuplicationEnhancerAdoption method, of class AnnotateCNVs.
     */
    @Test
    public void testTandemDuplicationEnhancerAdoption() {
        System.out.println("tandemDuplicationEnhancerAdoption");
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();        
        GenomicSet<CNV> dups = exampleData.getDups();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancers = exampleData.getEnhancer();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        
        AnnotateCNVs.boundaryOverlap(dups, boundaries); 
        AnnotateCNVs.annoateOverlap(dups, boundaries, genes, enhancers, phenotypeData);
        AnnotateCNVs.overlapPhenogramScore(dups, phenotypeData);
        AnnotateCNVs.defineOverlappedDomainRegions(dups, domains);

        AnnotateCNVs.tandemDuplicationEnhancerAdoption(dups, genes, enhancers, phenotypeData);
        
        System.out.println(dups.get("dup1"));
        System.out.println(dups.get("dup1").getBoundaryOverlap());
        System.out.println(dups.get("dup1").getLeftOverlappedDomainRegion());
        System.out.println(dups.get("dup1").getRightOverlappedDomainRegion());
                
        // dup1 is TanDupEA
        assertEquals("TanDupEA", dups.get("dup1").getEffectMechanism("TanDupEA"));

        System.out.println(dups.get("dup2"));
        System.out.println(dups.get("dup2").getBoundaryOverlap());
        System.out.println(dups.get("dup2").getLeftOverlappedDomainRegion());
        System.out.println(dups.get("dup2").getRightOverlappedDomainRegion());
        System.out.println(dups.get("dup2").getOverlapPhenogramScore());

        // dup2 is only GDE
        assertEquals("onlyGDE", dups.get("dup2").getEffectMechanism("TanDupEA"));
    }

    /**
     * Test of annoateOverlap method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnoateOverlap() {
        System.out.println("annoateOverlap");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> enhancers = exampleData.getEnhancer();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();

        AnnotateCNVs.annoateOverlap(cnvs, boundaries, genes, enhancers, phenotypeData);
        
        // ovelaped genes of cnv1 is only geneB and geneD (becasue any overlap is relevant here)
        GenomicSet<Gene> cnv1genes = new GenomicSet<Gene>();
        cnv1genes.put("geneB", genes.get("geneB"));
        cnv1genes.put("geneD", genes.get("geneD"));
        assertEquals(cnv1genes, cnvs.get("cnv1").getGenesInOverlap());
        
        // cvn4 does not overlap a boundary completely
        assertEquals(new GenomicSet<GenomicElement>(), cnvs.get("cnv4").getBoundaryOverlap());
        
        // cnv1 overlaps only genes B and D with phenomatch scores each of 0.29
        assertEquals(0.29, cnvs.get("cnv1").getOverlapPhenogramScore(), 0.01);

        // cnv4 does not overlap any gene and has therfore a score of 0.0
        assertEquals(0.0, cnvs.get("cnv4").getOverlapPhenogramScore(), 0.01);
        
    }

    /**
     * Test of annoateAdjacentRegions method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnoateAdjacentRegions() {
        System.out.println("annoateAdjacentRegions");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> enhancers = exampleData.getEnhancer();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        
        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        AnnotateCNVs.annoateAdjacentRegions(cnvs, genes, enhancers, phenotypeData);

        // right adjacent genes of cnv1 is only geneA
        GenomicSet<Gene> cnv1rAdj = new GenomicSet<Gene>();
        cnv1rAdj.put("geneA", genes.get("geneA"));
        assertEquals(cnv1rAdj, cnvs.get("cnv1").getGenesInRightRegion());
        
        // left adjacent region of cnv2 is the interval [0,8)
        assertEquals(new GenomicElement("chr1", 0, 8, "leftAdjacentRegion"), cnvs.get("cnv2").getLeftAdjacentRegion());        
        
    }

    /**
     * Test of annotateOverlappedGenes method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateOverlappedGenes() {
        System.out.println("annotateOverlappedGenes");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();

        AnnotateCNVs.annotateOverlappedGenes(cnvs, genes);

        // ovelaped genes of cnv1 is only geneB and geneD (becasue any overlap is relevant here)
        GenomicSet<Gene> cnv1genes = new GenomicSet<Gene>();
        cnv1genes.put("geneB", genes.get("geneB"));
        cnv1genes.put("geneD", genes.get("geneD"));
        assertEquals(cnv1genes, cnvs.get("cnv1").getGenesInOverlap());
        
        // cnv4 does not ovelrap any gene
        assertEquals(new GenomicSet<Gene>(), cnvs.get("cnv4").getGenesInOverlap());
    }

    /**
     * Test of defineAdjacentRegionsByDistance method, of class AnnotateCNVs.
     */
    @Test
    public void testDefineAdjacentRegionsByDistance() {
        System.out.println("defineAdjacentRegionsByDistance");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();

        int regionSize = 4;
        
        AnnotateCNVs.defineAdjacentRegionsByDistance(cnvs, regionSize);
        
        assertEquals(new GenomicElement("chr1", 4, 8, "leftAdjacent"), cnvs.get("cnv2").getLeftAdjacentRegion());
        assertEquals(new GenomicElement("chr1", 33, 37, "rightAdjacent"), cnvs.get("cnv2").getRightAdjacentRegion());
        
    }

    /**
     * Test of annotateAdjacentGenes method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateAdjacentGenes() {
        System.out.println("annotateAdjacentGenes");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<Gene> genes = exampleData.getGenes();

        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        AnnotateCNVs.annotateAdjacentGenes(cnvs, genes);

        // right adjacent genes of cnv1 is only geneA
        GenomicSet<Gene> cnv1rAdj = new GenomicSet<Gene>();
        cnv1rAdj.put("geneA", genes.get("geneA"));
        assertEquals(cnv1rAdj, cnvs.get("cnv1").getGenesInRightRegion());
        
    }

    /**
     * Test of overlapPhenogramScore method, of class AnnotateCNVs.
     */
    @Test
    public void testOverlapPhenogramScore() {
        System.out.println("overlapPhenogramScore");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        AnnotateCNVs.overlapPhenogramScore(cnvs, phenotypeData);

        // cnv1 overlaps only genes B and D with phenomatch scores each of 0.29
        assertEquals(0.29, cnvs.get("cnv1").getOverlapPhenogramScore(), 0.01);
    
        // cnv4 does not overlap any gene
        assertEquals(0.0, cnvs.get("cnv4").getOverlapPhenogramScore(), 0.01);

    }

    /**
     * Test of adjacentPhenogramScore method, of class AnnotateCNVs.
     */
    @Test
    public void testAdjacentPhenogramScore() {
        System.out.println("adjacentPhenogramScore");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<Gene> genes = exampleData.getGenes();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();

        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        AnnotateCNVs.annotateAdjacentGenes(cnvs, genes);
        AnnotateCNVs.adjacentPhenogramScore(cnvs, phenotypeData);

        // cnv1 has gne A with score 1.68 on the left and gene C with score 0 on the right
        assertEquals(1.68, cnvs.get("cnv1").getRightAdjacentPhenogramScore(), 0.01);
        assertEquals(0.0, cnvs.get("cnv1").getLeftAdjacentPhenogramScore(), 0.01);
    
    }

    /**
     * Test of annotateAdjacentEnhancers method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateAdjacentEnhancers() {
        System.out.println("annotateAdjacentEnhancers");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        GenomicSet<GenomicElement> enhancers = exampleData.getEnhancer();

        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        AnnotateCNVs.annotateAdjacentEnhancers(cnvs, enhancers);
        
        // set with only e_1 
        GenomicSet<GenomicElement> ehLeft = new GenomicSet<GenomicElement>();
        ehLeft.put("e_1", enhancers.get("e_1")); 

        // set with only e_2 
        GenomicSet<GenomicElement> ehRight = new GenomicSet<GenomicElement>();
        ehRight.put("e_2", enhancers.get("e_2")); 
        
        assertEquals(ehLeft, cnvs.get("cnv1").getEnhancersInLeftRegion());
        assertEquals(ehRight, cnvs.get("cnv1").getEnhancersInRightRegion());
        
    }

    /**
     * Test of annotateTDBDjustByScore method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateTDBDjustByScore() {
        System.out.println("annotateTDBDjustByScore");
        
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();

        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);
        AnnotateCNVs.defineAdjacentRegionsByDomains(cnvs, domains);
        AnnotateCNVs.annoateOverlap(cnvs, boundaries, genes, enhancer, phenotypeData);
        AnnotateCNVs.annoateAdjacentRegions(cnvs, genes, enhancer, phenotypeData);

        AnnotateCNVs.annotateTDBDjustByScore(cnvs);
        
        assertEquals("TDBD", cnvs.get("cnv1").getEffectMechanism("newTDBD"));
        assertEquals("GDE", cnvs.get("cnv2").getEffectMechanism("newTDBD"));
        assertEquals("TDBD", cnvs.get("cnv3").getEffectMechanism("newTDBD"));
        assertEquals("NoData", cnvs.get("cnv4").getEffectMechanism("newTDBD"));
    }

    /**
     * Test of annotateEA method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateEA() {
        System.out.println("annotateEA");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        HashMap<Term, HashSet<String>> term2genes = exampleData.getTargetTerm2targetGene();
        
        int regionSize=20;
        
        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);
        AnnotateCNVs.defineAdjacentRegionsByDistance(cnvs, regionSize);
        AnnotateCNVs.annoateOverlap(cnvs, boundaries, genes, enhancer, phenotypeData);
        AnnotateCNVs.annoateAdjacentRegions(cnvs, genes, enhancer, phenotypeData);


        AnnotateCNVs.annotateEA(cnvs, term2genes);

        assertEquals("EA", cnvs.get("cnv1").getEffectMechanism("EA"));
    }

    /**
     * Test of annotateEAlowG method, of class AnnotateCNVs.
     */
    @Test
    public void testAnnotateEAlowG() {
        System.out.println("annotateEAlowG");
        GenomicSet<CNV> cnvs = exampleData.getCnvs();
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();
        HashMap<Term, HashSet<String>> term2genes = exampleData.getTargetTerm2targetGene();
        int regionSize=20;
        
        AnnotateCNVs.boundaryOverlap(cnvs, boundaries);
        AnnotateCNVs.defineAdjacentRegionsByDistance(cnvs, regionSize);
        AnnotateCNVs.annoateOverlap(cnvs, boundaries, genes, enhancer, phenotypeData);
        AnnotateCNVs.annoateAdjacentRegions(cnvs, genes, enhancer, phenotypeData);

        AnnotateCNVs.annotateEAlowG(cnvs, term2genes);

        assertEquals("EAlowG", cnvs.get("cnv1").getEffectMechanism("EAlowG"));

    }

    /**
     * Test of inversionEnhancerAdoption method, of class AnnotateCNVs.
     */
    @Test
    public void testInversionEnhancerAdoption() {
        System.out.println("inversionEnhancerAdoption");
        GenomicSet<CNV> inversions = exampleData.getInvs();
        
        System.out.println("DEBUG" + inversions);
        
        GenomicSet<Gene> genes = exampleData.getGenes();
        GenomicSet<GenomicElement> enhancer = exampleData.getEnhancer();
        System.out.println("DEBUG" + enhancer);
        GenomicSet<GenomicElement> boundaries = exampleData.getBoundaries();
        GenomicSet<GenomicElement> domains = exampleData.getDomains();
        PhenotypeData phenotypeData = exampleData.getPhenotypeData();

        AnnotateCNVs.boundaryOverlap(inversions, boundaries);
        AnnotateCNVs.defineAdjacentRegionsByDomains(inversions, domains);
        AnnotateCNVs.defineOverlappedDomainRegions(inversions, domains);
        AnnotateCNVs.annoateOverlap(inversions, boundaries, genes, enhancer, phenotypeData);
        AnnotateCNVs.annoateAdjacentRegions(inversions, genes, enhancer, phenotypeData);
        
        System.out.println("DEBUG inv2 left reg:" + inversions.get("inv2").getLeftAdjacentRegion());
        System.out.println("DEBUG inv2 left e:" + inversions.get("inv2").getEnhancersInLeftRegion());
        
        AnnotateCNVs.inversionEnhancerAdoption(inversions, genes, enhancer, phenotypeData);
        System.out.print("DEBUG invs:" + inversions);
        assertEquals("EnhancerInvEA", inversions.get("inv1").getEffectMechanism("InvEA"));
        assertEquals("GeneInvEA", inversions.get("inv2").getEffectMechanism("InvEA"));
        assertEquals("noInvEA", inversions.get("inv3").getEffectMechanism("InvEA"));
    }

}
