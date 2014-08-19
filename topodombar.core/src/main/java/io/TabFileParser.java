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

package io;

import genomicregions.CNV;
import genomicregions.Gene;
import genomicregions.GenomicElement;
import genomicregions.GenomicSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import ontologizer.go.Term;
import phenotypeontology.PhenotypeData;
import phenotypeontology.TargetTerm;


/**
 * Reads a tab-separated file with genomic elements like CNVs or enhancers 
 into an GenomicSet object.
 * 
 * @author Jonas Ibn-Salem <ibnsalem@molgen.mpg.de>
 */
public class TabFileParser {
    
    /** {@link Path} to the file that is read by this parser */
    private final Path path;
    
    /** indicates that the IDs in column 4 of the input file are unique.*/
    private boolean uniqueIDs;
        
    /**
     * Construct a {@code TabFileParser} object from an input path.
     * The constructor creates a {@link Path} object from the input String.
     * 
     * @param strPath  path to input file.
     */
    public TabFileParser(String strPath) {
        this.path = Paths.get(strPath);
        
        // check for unqiue ID in input file
        try{
            this.uniqueIDs = checkForUniqueIDs();
        }catch (IOException e){
            System.out.println("[ERROR]  " + e);
            System.exit(1);
        }        
    }
    
    /**
     * Reads a TAB separated file with genomic elements like enhancers or topological domains.
     * Assumes each line in the file to represent a genomic element. The first
     * three columns should contain the following: chromosome, start and end.
     * Note, the genomic coordinates are assumed in 0-based half-open format 
     * like in the BED format specifications 
     * (See http://genome.ucsc.edu/FAQ/FAQformat.html#format1).
     * 
     * @return {@link GenomicSet} with all elements from the input file.
     * 
     * @throws IOException if file can not be read. 
     */
    public GenomicSet<GenomicElement> parse() throws IOException{
        
        // construct new set of genomic reigons:
        GenomicSet ges = new GenomicSet();
        
        // count IDs to give each ID a unique identifier, this will only be used
        // in case of non-unique IDs
        HashMap<String, Integer> countIDs = new HashMap<String, Integer>();
        
        try{
            for ( String line : Files.readAllLines( path, StandardCharsets.UTF_8 ) ){

                // split line by TABs
                String [] cols = line.split("\t");

                // if line contains too few columns:
                if (cols.length < 3 ){
                    throw new IOException(String.format(
                            "[ERROR] while reading file '%s'. Wrong number of "
                                    + "columns in input line: '%s'", path, line));
                }

                String chr = cols[0];
                int start = Integer.parseInt(cols[1]);
                int end = Integer.parseInt(cols[2]);
                
                // if ID column is available take name from it, else defaults to "ID"
                String name = cols.length >= 4 ? cols[3] : "ID";
                
                // In case of non-unique IDs, check if the name is already contained
                if (! this.uniqueIDs){
                    if (countIDs.containsKey(name)){
                        // increase counter
                        countIDs.put(name, countIDs.get(name)+1);
                        // append a number to the name to have a unique ID
                        name = name + "_" + countIDs.get(name).toString();
                    // if the name is seen the first time
                    }else{
                        // count it and append a 1
                        countIDs.put(name, 1);
                        name = name + "_1";
                    }
                }
                // create new element and add it to the set
                ges.put(name, new GenomicElement(chr, start, end, name));
            }
        }
        catch (IOException e){
            System.err.println("Error while parsing file:" + this.path);
            throw e;
        }
        return ges;
    }
    
    /**
     * Reads a TAB separated file with CNVs.
     * Assumes each line in the file to represent a CNV. The first
     * four columns should contain the following: chromosome, start, end, and name.
     * Three additional columns are are assumed. the fifth column should contain the CNV type.
     * In the sixth column we assume the 
     * phenotypes annotation of the patient in HPO terms separated by semicolon ';'.
     * The seventh column should contain the so called target term, a single more general 
     * HPO term that represent the more abstract phenotypical class ore tissue 
     * type to which the patient belongs to.
     * Note, the genomic coordinates are assumed in 0-based half-open format 
     * like in the BED format specifications 
     * (See http://genome.ucsc.edu/FAQ/FAQformat.html#format1).
     * 
     * @return {@link GenomicSet} with {@link CNV} objects from the input file.
     * 
     * @throws IOException if file can not be read. 
     */
    public GenomicSet<CNV> parseCNV() throws IOException{
        
        // construct new set ofCNVs:
        GenomicSet<CNV> cnvs = new GenomicSet();

        // count IDs to give each ID a unique identifier, this will only be used
        // in case of non-unique IDs
        HashMap<String, Integer> countIDs = new HashMap<String, Integer>();
        
        for ( String line : Files.readAllLines( path, StandardCharsets.UTF_8 ) ){
            
            // split line by TABs
            String [] cols = line.split("\t");
            
            // if line contains too few columns:
            if (cols.length < 4 ){
                throw new IOException(String.format(
                        "[ERROR] while reading file '%s'. Wrong number of "
                                + "columns in input line: '%s'", path, line));
            }

            // parse columns
            String chr = cols[0];
            int start = Integer.parseInt(cols[1]);
            int end = Integer.parseInt(cols[2]);
            String name = cols[3];

            // In case of non-unique IDs, check if the name is already contained
            if (! this.uniqueIDs){
                if (countIDs.containsKey(name)){
                    // increase counter
                    countIDs.put(name, countIDs.get(name)+1);
                    // append a number to the name to have a unique ID
                    name = name + "_" + countIDs.get(name).toString();
                // if the name is seen the first time
                }else{
                    // count it and append a 1
                    countIDs.put(name, 1);
                    name = name + "_1";
                }
            }

            // parse CNV specific columns if available, set default else
            String type = ".";
            if (cols.length >= 5){
                type =  cols[4];
            }
            
            // create new {@link CNV} object
            CNV cnv = new CNV(chr, start, end, name, type);
            
            // add it to the set
            cnvs.put(name, cnv);
        }
        
        return cnvs;
    }

    /**
     * Reads a TAB separated file with CNVs.
     * Assumes each line in the file to represent a CNV. The first
     * four columns should contain the following: chromosome, start, end, and name.
     * Three additional columns are are assumed. the fifth column should contain the CNV type.
     * In the sixth column we assume the 
     * phenotypes annotation of the patient in HPO terms separated by semicolon ';'.
     * The seventh column should contain the so called target term, a single more general 
     * HPO term that represent the more abstract phenotypical class ore tissue 
     * type to which the patient belongs to.
     * Note, the genomic coordinates are assumed in 0-based half-open format 
     * like in the BED format specifications 
     * (See http://genome.ucsc.edu/FAQ/FAQformat.html#format1).
     * This function builds Term objects for all phenotype terms used to annotate the patient.
     * Therefore the phenotype ontology has to be given as additional argument
     * as an {@link PhenotypeData} object.
     * 
     * @param phenotypeData the phenotype ontology 
     * 
     * @return {@link GenomicSet} with {@link CNV} objects from the input file.
     * 
     * @throws IOException if file can not be read. 
     */
    public GenomicSet<CNV> parseCNVwithPhenotypeAnnotation(PhenotypeData phenotypeData) throws IOException{
                
        // construct new set ofCNVs:
        GenomicSet<CNV> cnvs = new GenomicSet<CNV>();
        
        // count IDs to give each ID a unique identifier, this will only be used
        // in case of non-unique IDs
        HashMap<String, Integer> countIDs = new HashMap<String, Integer>();

        for ( String line : Files.readAllLines( path, StandardCharsets.UTF_8 ) ){
            
            // split line by TABs
            String [] cols = line.split("\t");

            // if line contains too few columns:
            if (cols.length < 7 ){
                throw new IOException(String.format(
                        "[ERROR] while reading file '%s'. Wrong number of "
                                + "columns in input line: '%s'", path, line));
            }
            
            // parse columns
            String chr = cols[0];
            int start = Integer.parseInt(cols[1]);
            int end = Integer.parseInt(cols[2]);
            String name = cols[3];
            
            // In case of non-unique IDs, check if the name is already contained
            if (! this.uniqueIDs){
                if (countIDs.containsKey(name)){
                    // increase counter
                    countIDs.put(name, countIDs.get(name)+1);
                    // append a number to the name to have a unique ID
                    name = name + "_" + countIDs.get(name).toString();
                // if the name is seen the first time
                }else{
                    // count it and append a 1
                    countIDs.put(name, 1);
                    name = name + "_1";
                }
            }
            
            // parse CNV specific columns if available, set default else
            String type = ".";
            if (cols.length >= 5){
                type =  cols[4];
            }
            
            // parse phenotypes as set of Term objects
            HashSet<Term> phenotypes = new HashSet<Term>();
            
            // for all term IDs in the phenotype column
            for (String termID : cols[5].split(";")){
                
                // consturct a phenotype Term object and add it to the set
                phenotypes.add(phenotypeData.getTermIncludingAlternatives(termID));
            }
            
            // create Term object form column with targetTerm ID
            Term targetTerm = phenotypeData.getTermIncludingAlternatives(cols[6]);
                
            // create new {@link CNV} object
            CNV cnv = new CNV(chr, start, end, name, type, phenotypes, targetTerm);
            
            // add it to the set
            cnvs.put(name, cnv);

        }
        return cnvs;
    }
    
    /**
     * Reads a TAB separated file with CNVs and assumes all patients to have the
     * same phenotype given by a single input phenotype term.
     * Assumes each line in the file to represent a CNV. The first
     * five columns should contain the following: chromosome, start, end, name, and type.
     * The input phenotype term will be used as patient specific annotation and
     * targetTerm.
     * Note, the genomic coordinates are assumed in 0-based half-open format 
     * like in the BED format specifications 
     * (See http://genome.ucsc.edu/FAQ/FAQformat.html#format1).
     * 
     * 
     * @param globalPhenotype single phenotype term for all input CNVs
     * @return {@link GenomicSet} with {@link CNV} objects from the input file.
     * 
     * @throws IOException if file can not be read. 
     */
    public GenomicSet<CNV> parseCNVwithGlobalTerm(Term globalPhenotype) throws IOException{
                
        // construct new set ofCNVs:
        GenomicSet<CNV> cnvs = new GenomicSet<CNV>();
        
        // count IDs to give each ID a unique identifier, this will only be used
        // in case of non-unique IDs
        HashMap<String, Integer> countIDs = new HashMap<String, Integer>();

        for ( String line : Files.readAllLines( path, StandardCharsets.UTF_8 ) ){
            
            // split line by TABs
            String [] cols = line.split("\t");
            // if line contains too few columns:
            if (cols.length < 4 ){
                throw new IOException(String.format(
                        "[ERROR] while reading file '%s'. Wrong number of "
                                + "columns in input line: '%s'", path, line));
            }
            
            // parse columns
            String chr = cols[0];
            int start = Integer.parseInt(cols[1]);
            int end = Integer.parseInt(cols[2]);
            String name = cols[3];

            // In case of non-unique IDs, check if the name is already contained
            if (! this.uniqueIDs){
                if (countIDs.containsKey(name)){
                    // increase counter
                    countIDs.put(name, countIDs.get(name)+1);
                    // append a number to the name to have a unique ID
                    name = name + "_" + countIDs.get(name).toString();
                // if the name is seen the first time
                }else{
                    // count it and append a 1
                    countIDs.put(name, 1);
                    name = name + "_1";
                }
            }
            
            // parse CNV specific columns if available, set default else
            String type = ".";
            if (cols.length >= 5){
                type =  cols[4];
            }
            
            // parse phenotypes as set of Term objects
            HashSet<Term> phenotypes = new HashSet<Term>();
            phenotypes.add(globalPhenotype);
                        
            // create new {@link CNV} object
            CNV cnv = new CNV(chr, start, end, name, type, phenotypes, globalPhenotype);
            
            // add it to the set
            cnvs.put(name, cnv);

        }
        return cnvs;
    }
    
    /**
     * Reads all target terms form the 6th column of an input CNV file.
     * 
     * @param phenotypeData
     * @return set of target terms.
     * @throws java.io.IOException
     */
    public HashSet<Term> parseTargetTermSet(PhenotypeData phenotypeData) throws IOException{
        
        HashSet<Term> targetTerms = new HashSet();
        
        for ( String line : Files.readAllLines( path, StandardCharsets.UTF_8 ) ){
            
            // split line by TABs
            String [] cols = line.split("\t");
            
            if (cols.length <= 6){
                throw new IOException(String.format("No column found for target"
                        + "Term by reading file '%s'. "
                        + "Column number <= 6 in input line.", path));
            }else{
        
                // targetTerm ID
               String termID = cols[6];
               
               targetTerms.add(phenotypeData.getTermIncludingAlternatives(termID));

            }
        }
        return targetTerms;
    }
        
    /**
     * Reads a TAB separated file with genes.
     * Assumes the .tab format form the barrier project.
     * Note, that the file should contain genes with unique Entrez Gene IDs in the
     * fourth column.
     * 
     * @return  a set of genes as {@link GenomicSet} of {@link Gene} objects.
     * @throws IOException if the file can not be read
     */
    public GenomicSet<Gene> parseGene() throws IOException{
        
        // construct new set of genomic reigons:
        GenomicSet<Gene> genes = new GenomicSet();
        
        // count IDs to give each ID a unique identifier, this will only be used
        // in case of non-unique IDs
        HashMap<String, Integer> countIDs = new HashMap<String, Integer>();

        for ( String line : Files.readAllLines( path, StandardCharsets.UTF_8 ) ){
            
            // split line by TABs
            String [] cols = line.split("\t");
            // if line contains too few columns:
            if (cols.length < 4 ){
                throw new IOException(String.format(
                        "[ERROR] while reading file '%s'. Wrong number of "
                                + "columns in input line: '%s'", path, line));
            }
            
            // parse columns
            String chr = cols[0];
            int start = Integer.parseInt(cols[1]);
            int end = Integer.parseInt(cols[2]);
            String name = cols[3];
            
            // In case of non-unique IDs, check if the name is already contained
            if (! this.uniqueIDs){
                if (countIDs.containsKey(name)){
                    // increase counter
                    countIDs.put(name, countIDs.get(name)+1);
                    // append a number to the name to have a unique ID
                    name = name + "_" + countIDs.get(name).toString();
                // if the name is seen the first time
                }else{
                    // count it and append a 1
                    countIDs.put(name, 1);
                    name = name + "_1";
                }
            }
            
            // create new {@link Gene} object
            Gene g = new Gene(chr, start, end, name);
            
            // parse Gene specific columns
            if (cols.length >= 5){
                g.setStrand( cols[4] );
            }else{
                g.setStrand(".");
            }
            //Gene Symbol is not contained in the .tab format of the barrier project 
            g.setSymbol("."); 
            // TODO: write an additional constructor and remove setter functions
            // add it to the set
            genes.put(name, g);
        }
        
        return genes;
    }

    public GenomicSet<Gene> parseGeneWithTerms(PhenotypeData phenotypeData) throws IOException{
        
        // use default parse function
        GenomicSet<Gene> genes = parseGene();
        
        // iterate over all genes
        for (String gID: genes.keySet()){

            Gene g = genes.get(gID);
            
            // test if for the genes, annotations are available
            if (phenotypeData.containsGene(gID)){
                // retrive the phenotypes form the phenotypeData object
                g.setPhenotypeTerms( phenotypeData.getGenePhenotypes(gID) );
            }else{
                // if no entry for the gene was found, create an empty set
                g.setPhenotypeTerms( new HashSet() );
            }
            
            genes.put(gID, g);
        }
        
        return genes;
    }

    /**
     * Parses the topological domains and computes boundary regions from them.
     * This method assumes non-overlapping domain regions. A boundary is defined
     * as a region between two adjacent domains, that is smaller or equal than 
     * 400 kb as in the Dixon et al. (2012) Nature publication.
     * For compatibility with the interval tree from the jannovar package, the
     * boundary regions will be artificially extended to one bp in case of
     * zero-length boundary elements.
     * 
     * @return {@link GenomicSet} with boundary regions between domains.
     * 
     * @throws IOException if file can not be read. 
     */
    public GenomicSet<GenomicElement> parseBoundariesFromDomains() throws IOException{
        
        GenomicSet<GenomicElement> boundaries = new GenomicSet();
        
        // parse topological domains with the default parse method:
        GenomicSet<GenomicElement> domains = parse();
        
        // sort domains by chromosom:
        HashMap<String, ArrayList<GenomicElement>> chr2elementList = new HashMap();
        for (GenomicElement d : domains.values()){
            
            String chr = d.getChr();
            
            // test if chr is not already in chr2element map:
            if (! chr2elementList.containsKey(chr)){
                chr2elementList.put(chr, new ArrayList<GenomicElement>());
            }
            
            // fill map:
            chr2elementList.get(chr).add(d);                        
        }
        
        // for each chromosome sort domains by start position
        for (String chr : chr2elementList.keySet()){
            
            ArrayList<GenomicElement> domainList = chr2elementList.get(chr);
            
            // sort domains by theire start coordinate
            Collections.sort(domainList, GenomicElement.START_COORDINATE_ORDER);
            
            // iterate over all adjacent domain pairs
            for(int i=0 ; i < domainList.size()-1 ; i++){
                
                GenomicElement dLeft = domainList.get(i);
                GenomicElement dRight = domainList.get(i+1);
                
                // distance between domains in bp
                int dist = dRight.getStart() - dLeft.getEnd() ;
                
                // consider only boundaries with size <= 400 kb
                if (dist <= 400000){
                    
                    // construct boundary element
                    String boundaryName = "b_" + (boundaries.size()+1);
                    int bStart = dLeft.getEnd();
                    int bEnd = dRight.getStart();
                    
                    // If boundary has length 0 it will be artifically extended to
                    // one bp to the right for proper interval overlap calculations.
                    if (bEnd - bStart == 0){
                        bEnd += 1;
                    }
                    GenomicElement b = new GenomicElement(chr, bStart, bEnd, boundaryName);
                    
                    // add boundary to the set
                    boundaries.put(boundaryName, b);
                }
            }
        }
        
        return boundaries;
    }
    
    /**
     * Reads the target terms file and returns a list of {@link TargetTerm} objects, 
     * that contains also the corresponding enhancer data.
     * 
     * @param phenotypeData phenotype data object to retrive the actual {@code Term} object from.
     * @return an {@link ArrayList} of {@link TargetTerm} objects 
     * @throws IOException 
     */
    public ArrayList<TargetTerm> parseTargetTerms(PhenotypeData phenotypeData) throws IOException{
        
        ArrayList<TargetTerm> targetTerms = new ArrayList<TargetTerm>();

        // iterate over all lines in the file
        for ( String line : Files.readAllLines(this.path, StandardCharsets.UTF_8 ) ){
            
            // ignore comments or header line
            if (line.startsWith("#")){
                continue;
            }
            
            // split line by TABs
            String [] cols = line.split("\t");
            
            // check if enougth columns are available
            if (cols.length < 3 ){
                throw new IOException(String.format(
                        "[ERROR] while reading file '%s'. Wrong number of "
                                + "columns in input line: '%s'"
                                + "\nExpect at leaste 3 columns but found only %d.", path, line, cols.length));
            }
            
            String termID = cols[0];
            String name = cols[1];
            String enhancerPathString = cols[2];
            
            // get Term object form phenotpye data.
            Term term = phenotypeData.getTermIncludingAlternatives(termID);
            
            // parse the enhancer data
            Path enhancerPath = Paths.get(enhancerPathString);
            
            // if path in target term file is relative
            if (! enhancerPath.isAbsolute()){

                // get the directory part of this target term file
                Path dir = this.path.getParent();
                
                //join it with the realtive path to the enhancer files
                enhancerPath = dir.resolve(enhancerPath);
            }
            
            // parse enhancer data
            TabFileParser enhancerParser = new TabFileParser(enhancerPath.toString());
            GenomicSet<GenomicElement> enhancers = enhancerParser.parse();
            
            // construct a new TargetTerm object and append it to the result list
            targetTerms.add(new TargetTerm(term, name, enhancers));
        
        }
        
        return targetTerms;
    }
    
    /**
     * This function tests if the IDs or names in column 4 are unique to each element.
     * @return true if the IDs are unique
     * @throws IOException 
     */
    private boolean checkForUniqueIDs() throws IOException{

        // set for unique ids in file
        HashSet<String> seenIDs = new HashSet<String>();
        
        for ( String line : Files.readAllLines( this.path, StandardCharsets.UTF_8 ) ){

            // ignore comment lines starting with "#";
            if (line.startsWith("#")) {
                continue;
            } else{
                
                // split line by TABs
                String [] cols = line.split("\t");
                // if line contains too few columns:
                if (cols.length < 3 ){
                    throw new IOException(String.format(
                            "[ERROR] while reading file '%s'. Wrong number of "
                                    + "columns in input line: '%s'", path, line));
                }
                
                // if no ID column is present, return false
                if (cols.length < 4){
                    return false;
                }
                
                String name = cols[3];
                
                // if name was seen already, return false
                if (seenIDs.contains(name)){
                    return false;
                }
                // add id to set of seen IDs
                seenIDs.add(name);
            }
        }
        
        // if no ID was seen before
        return true;
    }
    
    
    
}
