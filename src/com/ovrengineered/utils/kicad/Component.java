package com.ovrengineered.utils.kicad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class Component
{		
	private final Map<String, String> fields;
	private final List<String> fieldNamesAndOrder;
	
	private final Integer bomLineNumber;
	
	
	private Component(List<String> fieldNames, String lineIn, Integer lineNumberIn)
	{
		this.fields = new HashMap<String, String>();
		this.fieldNamesAndOrder = fieldNames;
		this.bomLineNumber = lineNumberIn;
		
		int currFieldIndex = 0;
		for(String currField : lineIn.split(",") )
		{
			// if we don't have a field name for it, we can't process it
			if( fieldNames.size() <= currFieldIndex ) return;
			
			String fieldName = fieldNames.get(currFieldIndex++);
			String fieldVal = currField.trim();
			
			this.fields.put(fieldName, fieldVal);
		}
	}
	
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for( String currFieldName : this.fieldNamesAndOrder )
		{
			String currFieldVal = this.fields.get(currFieldName);
			if( currFieldVal != null ) sb.append(currFieldVal);
			sb.append(",");
		}
		return sb.toString();
	}
	
	
	public static List<Component> parseFromFileAtPath(Path pathIn) throws IOException
	{
		List<Component> retVal = new ArrayList<Component>();
		
		Stream<String> strStream = Files.lines(pathIn);
		Iterator<String> it = strStream.iterator();
		
		// parse our field names
		if( !it.hasNext() )
		{
			// don't have _any_ lines in this field
			strStream.close();
			return null;
		}
		String headerLine = it.next();
		List<String> fieldNames = new ArrayList<String>();
		for(String currFieldName : headerLine.split(","))
		{
			fieldNames.add(currFieldName.trim());
		}
		
		// now parse our components
		int lineNum = 0;
		while( it.hasNext() )
		{
			retVal.add(new Component(fieldNames, it.next(), lineNum++));
		}
		
		// all done
		strStream.close();
		return retVal;
	}
	
	
	public static void exportComponentsToPath(List<Component> componentsIn, Path pathIn) throws IOException
	{
		if( (componentsIn == null) || (componentsIn.size() == 0) ) return;
		
		List<String> outputLines = new ArrayList<String>();
		
		// create our header first
		StringBuilder sb = new StringBuilder();
		for( String currFieldName : componentsIn.get(0).fieldNamesAndOrder )
		{
			sb.append(currFieldName);
			sb.append(",");
		}
		outputLines.add(sb.toString());
		
		// add our components
		for( Component currComp : componentsIn )
		{
			outputLines.add(currComp.toString());
		}
		
		// output
		Files.write(pathIn, outputLines, StandardCharsets.UTF_8);
	}
	
	
	public static void exportComponentsToPathsBySupplier(List<Component> componentsIn, String fieldNameIn, Path pathIn) throws IOException
	{
		// separate out our components
		Map<String, List<Component>> componentsBySupplier = new HashMap<String, List<Component>>();
		for( Component currComp : componentsIn )
		{
			String compSupplierName = currComp.fields.get(fieldNameIn);
			if( compSupplierName == null )
			{
				System.err.println(String.format("Malformed supplier on line %d", currComp.bomLineNumber));
				continue;
			}
				
			// get our list of components for this supplier (or create if it doesn't exist)
			List<Component> componentsForThisSupplier = componentsBySupplier.get(compSupplierName);
			if( componentsForThisSupplier == null )
			{
				componentsForThisSupplier = new ArrayList<Component>();
				componentsBySupplier.put(compSupplierName, componentsForThisSupplier);
			}
			
			// add our component
			componentsForThisSupplier.add(currComp);
		}
		
		// now output each list
		Iterator<Entry<String, List<Component>>> it = componentsBySupplier.entrySet().iterator();
		while( it.hasNext() )
		{
			Entry<String, List<Component>> currEntry = it.next();
			
			// add the supplier name to differentiate
			ArrayList<String> fileNameParts = new ArrayList<String>(Arrays.asList(pathIn.getFileName().toString().split("\\.")));
			fileNameParts.add(1, "-" + (currEntry.getKey().equals("") ? "unknown" : currEntry.getKey()));
			if( fileNameParts.size() >= 3 ) fileNameParts.add(fileNameParts.size()-1, ".");
			
			StringBuilder sb = new StringBuilder();
			for( String currPart : fileNameParts )
			{
				sb.append(currPart);
			}
			
			Component.exportComponentsToPath(currEntry.getValue(), pathIn.resolveSibling(sb.toString()));
		}
	}
	
	
	public static List<Component> concatComponentsByFieldValue(List<Component> componentsIn, String fieldNameIn, String quantityFieldNameIn, String referenceFieldNameIn)
	{
		List<Component> retVal = new ArrayList<Component>();
		
		Iterator<Component> it = componentsIn.iterator();
		while( it.hasNext() )
		{
			Component currComp = it.next();
			String field_currComp = currComp.fields.get(fieldNameIn);
			
			// iterate through our existing retVal components to see if it's a duplicate
			boolean hasMatched = false;
			for( Component currExistingComp : retVal )
			{
				String field_currExistingComp = currExistingComp.fields.get(fieldNameIn);
				if( (field_currExistingComp != null) && 
					(field_currComp != null) &&
					field_currExistingComp.equals(field_currComp) )
				{
					// we have a match for this component...increment the quantity field (if asked to)
					if( quantityFieldNameIn != null )
					{						
						Integer currQty = Integer.valueOf(currExistingComp.fields.get(quantityFieldNameIn));
						currQty++;
						currExistingComp.fields.put(quantityFieldNameIn, currQty.toString());
					}
					
					// concat the references (if asked to)
					if( referenceFieldNameIn != null )
					{
						String currRefs = currExistingComp.fields.get(referenceFieldNameIn);
						currRefs += " " + currComp.fields.get(referenceFieldNameIn);
						currExistingComp.fields.put(referenceFieldNameIn,  currRefs);
					}
					
					// delete current component
					it.remove();
					
					hasMatched = true;
					break;
				}
			}
			
			if( !hasMatched ) 
			{
				if( field_currComp == null )
				{
					// 0-based -> 1-based + header row = +2
					System.err.println(String.format("Warning: Component on line %d does not have concat field '%s'", 
							currComp.bomLineNumber+2, fieldNameIn));
				}
				
				// this is a unique component...add a quantity field anyways
				// if it hasn't been added already (since all components
				// effectively share the same 'fieldNamesAndOrder' map
				if( !currComp.fieldNamesAndOrder.contains(quantityFieldNameIn) )
				{
					currComp.fieldNamesAndOrder.add(quantityFieldNameIn);
				}
				
				// now add the field for the component
				currComp.fields.put(quantityFieldNameIn, "1");
				retVal.add(currComp);
				System.out.println(String.format("new unique component on line %d", currComp.bomLineNumber+2));
			}
		}
		
		return retVal;
	}
}
