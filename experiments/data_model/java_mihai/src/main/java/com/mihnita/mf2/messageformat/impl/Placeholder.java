package com.mihnita.mf2.messageformat.impl;

import java.text.Format;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;
import com.mihnita.mf2.messageformat.datamodel.IPlaceholder;
import com.mihnita.mf2.messageformat.datamodel.functions.IPlaceholderFormatter;

public class Placeholder implements IPlaceholder {
	private final String name;
	private final String formatter_name;
	private final Map<String, String> options;

	 
	static class FormatDateTime implements IPlaceholderFormatter {
		@Override
		public String format(Object value, String locale, Map<String, String> options) {
			if (value instanceof Date) {
				String skeleton = options.get("skeleton");
				skeleton = skeleton == null ? "::dMMMMy" : "::" + skeleton;
				return DateFormat.getInstanceForSkeleton(skeleton, ULocale.forLanguageTag(locale)).format((Date) value);
			}
			return null;
		}
	}

	static class FormatNumber implements IPlaceholderFormatter {
		@Override
		public String format(Object value, String locale, Map<String, String> options) {
			ULocale ulocale = ULocale.forLanguageTag(locale);
		    Format newFormat = null;
			if (value instanceof CurrencyAmount) {
				newFormat = NumberFormat.getCurrencyInstance(ulocale);
			}
			if (value instanceof Number) {
				Number nValue = (Number) value;
			    String style = options == null ? "" : options.get("style");
			    if (style == null)
			    	style = "";
			    switch (style) {
			    	case "currency":
			    		String currencyCode = options == null ? null : options.get("currency_code");
			    		if (currencyCode != null && !currencyCode.isEmpty()) {
			    			value = new CurrencyAmount(nValue, Currency.getInstance(currencyCode));
			    		}
			    		newFormat = NumberFormat.getCurrencyInstance(ulocale);
			    		break;
			    	case "percent":
			    		newFormat = NumberFormat.getPercentInstance(ulocale);
			    		break;
			    	case "integer":
			    		newFormat = NumberFormat.getIntegerInstance(ulocale);
			    		break;
			    	default: // pattern or skeleton
			    		// Ignore leading whitespace when looking for "::", the skeleton signal sequence
			    		if (style == null || style.isEmpty()) {
				    		newFormat = NumberFormat.getInstance(ulocale);
			    		} else if (style.startsWith("::")) { // Skeleton
			    			newFormat = NumberFormatter.forSkeleton(style.substring(2)).locale(ulocale).toFormat();
			    		} else { // Pattern
			    			newFormat = new DecimalFormat(style, new DecimalFormatSymbols(ulocale));
			    		}
			    }
			}
			if (newFormat != null)
				return newFormat.format(value).toString();
			return null;
		}
	}

	final static Map<String, IPlaceholderFormatter> _defaultFormatterFunctions = new HashMap<>();
	static {
		_defaultFormatterFunctions.put("date", new FormatDateTime());
		_defaultFormatterFunctions.put("time", new FormatDateTime());
		_defaultFormatterFunctions.put("number", new FormatNumber());
	}

	public Placeholder(String name) {
		this(name, null, null);
	}

	public Placeholder(String name, String formatter_name) {
		this(name, formatter_name, null);
	}

	public Placeholder(String name, String formatter_name, Map<String, String> options) {
		super();
		this.name = name;
		this.formatter_name = formatter_name;
		this.options = options;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String formatter_name() {
		return formatter_name;
	}

	@Override
	public Map<String, String> options() {
		return options;
	}

	@Override
	public String format(String locale, Map<String, Object> parameters) {
		Object value = parameters.get(name);
		String result = null;
		IPlaceholderFormatter formatterFunction = _defaultFormatterFunctions.get(formatter_name);
		if (formatterFunction != null) {
			result = formatterFunction.format(value, locale, options);
		} else if (value != null) {
			result = value.toString();
		}
		return result == null ? "<undefined " + name + ">" : result;
	}
}