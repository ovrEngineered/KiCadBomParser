package com.ovrengineered.utils.kicad;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import com.ovrengineered.commandLineParser.*;
import com.ovrengineered.commandLineParser.optionListener.OptionNoArgumentListener;
import com.ovrengineered.commandLineParser.optionListener.OptionWithArgumentListener;


public class KiCadBomParser
{
	private enum Action
	{
		NO_OP,
		CONCAT,
		SPLIT_SUPPLIER
	}
	
	
	private static Path inputFile = null;
	private static Path outputFile = null;
	
	private static Action currAction = Action.NO_OP;
	
	private static String fieldName_concat = null;
	private static String fieldName_quantity = null;
	private static String fieldName_reference = null;
	
	private static String fieldName_splitSupp = null;
	
	
	public static void main(String[] args) throws IOException
	{
		CommandLineParser parser = new CommandLineParser("kicadBomParser", "Program that performs various operations on a bom exported from KiCAD");
		
		// Basic options
		parser.addOption("i", "input", "the original BOM (csv format)", true, true, String.class, new OptionWithArgumentListener<String>()
		{
			@Override
			public void optionIsPresent(String arg0)
			{
				inputFile = FileSystems.getDefault().getPath(arg0);
			}
		});
		parser.addOption("o", "output", "the output BOM (csv format)", false, true, String.class, new OptionWithArgumentListener<String>()
		{
			@Override
			public void optionIsPresent(String arg0)
			{
				outputFile = FileSystems.getDefault().getPath(arg0);
			}
		});
		
		// Concat options
		parser.addOption("c", "concat", "Action -> concatenate identical parts using passed field name to check for equality", false, true, String.class, new OptionWithArgumentListener<String>()
		{
			@Override
			public void optionIsPresent(String arg0)
			{
				currAction = Action.CONCAT;
				fieldName_concat = arg0;
			}
		});
		parser.addOption("q", "quantFieldName", "field name for quantity field", false, true, String.class, new OptionWithArgumentListener<String>()
		{
			@Override
			public void optionIsPresent(String arg0)
			{
				fieldName_quantity = arg0;
			}
		});
		parser.addOption("r", "refFieldName", "field name for reference field", false, true, String.class, new OptionWithArgumentListener<String>()
		{
			@Override
			public void optionIsPresent(String arg0)
			{
				fieldName_reference = arg0;
			}
		});
		
		// Split suppliers options
		parser.addOption("s", "splitSupp", "Action -> splits the BOMs into multiple files based upon supplier", false, true, String.class, new OptionWithArgumentListener<String>()
		{
			@Override
			public void optionIsPresent(String arg0)
			{
				currAction = Action.SPLIT_SUPPLIER;
				fieldName_splitSupp = arg0;
			}
		});
		
		// parse our options, return on failure
		if( !parser.parseOptions(args) ) return;
		
		// make sure we have an action
		switch( currAction )
		{
			case CONCAT:
				action_concat();
				break;
				
			case SPLIT_SUPPLIER:
				action_splitSupplier();
				break;
			
			default:
				action_noop();
				return;
		}
		
		System.out.println("complete");
		
	}
	
	
	private static void action_noop() throws IOException
	{
		List<Component> components = Component.parseFromFileAtPath(inputFile);
		Component.exportComponentsToPath(components, outputFile);
	}
	
	
	private static void action_concat() throws IOException
	{
		List<Component> components = Component.parseFromFileAtPath(inputFile);
		
		Component.concatComponentsByFieldValue(components, fieldName_concat, fieldName_quantity, fieldName_reference);
		
		Component.exportComponentsToPath(components, outputFile);
	}
	
	
	private static void action_splitSupplier() throws IOException
	{
		List<Component> components = Component.parseFromFileAtPath(inputFile);
		
		Component.exportComponentsToPathsBySupplier(components, fieldName_splitSupp, outputFile);
	}

}
