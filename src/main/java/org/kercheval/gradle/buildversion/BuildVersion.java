package org.kercheval.gradle.buildversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class BuildVersion
{
	//
	// Pattern used for date/time (not modifiable)
	//
	public static final String DATE_FORMAT_PATTERN = "yyyyMMdd";
	public static final String TIME_FORMAT_PATTERN = "HHmmss";

	//
	// The default pattern uses major, minor and standard maven time format
	//
	public static final String DEFAULT_PATTERN = "%M%.%m%-%d%.%t%";

	//
	// The actual version info pulled from the candidate based on the
	// pattern or passed into the constructor
	//
	private int major = 0;
	private int minor = 0;
	private int build = 0;

	private Date buildDate = new Date();
	//
	// These values are set based on the presence of variables in the
	// pattern in use. Set in validatePattern.
	//
	private boolean useMajor = false;
	private boolean useMinor = false;

	private boolean useBuild = false;

	//
	// Pattern used for version. The pattern must be set at follows...
	// - May not have any whitespace (validated)
	// - May contain any of the following variables (at most once)
	// %M% - major version
	// %m% - minor version
	// %b% - build number
	// %d% - date (using yyyyMMdd)
	// %t% - time (using HHmmss)
	// %% - a percent character (may appear multiple times in the pattern)
	//
	private String pattern;

	//
	// The validate pattern is used to verify candidate strings and is used
	// to verify toString output. This pattern is auto-generated if a specific
	// pattern is not supplied (based on output pattern).
	//
	private String validatePattern;

	//
	// The useLocalTimeZone variable determines what time zone is used to
	// set the pattern time zone. If true, the local machine timezone is
	// used, if false, the UTC timezone is used.
	//
	private boolean useLocalTimeZone;

	//
	// Create a default version
	//
	public BuildVersion(final String pattern)
		throws ParseException
	{
		this(pattern, null, null, false);
	}

	public BuildVersion(final String pattern,
		final int major,
		final int minor,
		final int build,
		final Date buildDate,
		final boolean useLocalTimeZone)
	{
		init(major, minor, build, buildDate, useLocalTimeZone);
		setPattern(pattern);
	}

	public BuildVersion(final String pattern, final String candidate, final boolean useLocalTimeZone)
		throws ParseException
	{
		this(pattern, null, candidate, useLocalTimeZone);
	}

	public BuildVersion(final String pattern,
		final String validatePattern,
		final String candidate,
		final boolean useLocalTimeZone)
		throws ParseException
	{
		init(0, 0, 0, null, useLocalTimeZone);
		setPattern(pattern, validatePattern);
		parseCandidate(candidate);
	}
	
	private static String generateValidatePattern(String buildPattern)
    {
        final StringBuilder validatePatternStr = new StringBuilder();

        //
        // Escape all regex meta characters while adding in the pattern variables
        //
        buildPattern = buildPattern.replaceAll("([\\\\*+\\[\\](){}\\$.?\\^|])", "\\\\$1");

        //
        // The pattern is known valid, just fill in the blanks
        //
        int index = 0;
        int lastIndex = index;

        while (index >= 0)
        {

            //
            // Find the next pattern block to validate
            //
            index = buildPattern.indexOf("%", index);

            int nextIndex = index + 3;

            if (index >= 0)
            {

                //
                // Place the block from the last match to the current into the string
                //
                validatePatternStr.append(buildPattern.substring(lastIndex, index));

                final char nextChar = buildPattern.charAt(index + 1);

                switch (nextChar)
                {
                case '%':
                    validatePatternStr.append("%");

                    //
                    // Backup the index one since this is only 2 characters consumed
                    //
                    nextIndex -= 1;

                    break;

                case 'M':
                case 'm':
                case 'b':
                case 'd':
                case 't':
                    validatePatternStr.append("\\d+");

                    break;

                default:

                    //
                    // This state is not possible if the validate method works. Not testable
                    // without breaking private contract
                    //
                    throw new IllegalStateException("Invalid pattern detected '" + buildPattern
                        + "' at index: " + index);
                }

                index = nextIndex;
                lastIndex = index;
            }
        }

        //
        // Tack on the postfix (if any)
        //
        validatePatternStr.append(buildPattern.substring(lastIndex));

        return validatePatternStr.toString();
    }

	private String checkPattern(final String checkPattern)
	{
		//
		// Reset variable usage for a new pattern
		//
		setUseMajor(false);
		setUseMinor(false);
		setUseBuild(false);

		//
		// Ensure the pattern contains no whitespace
		//
		if (checkPattern.matches("\\S*\\s+\\S*"))
		{
			throw new IllegalArgumentException("Invalid pattern: whitespace not allowed in pattern");
		}

		//
		// Ensure each pattern type is used zero or one times only
		//
		if (checkPattern.matches("\\S*%M%\\S*%M%\\S*"))
		{
			throw new IllegalArgumentException(
				"Invalid pattern: Major variable %M% used more than once in pattern");
		}

		if (checkPattern.matches("\\S*%m%\\S*%m%\\S*"))
		{
			throw new IllegalArgumentException(
				"Invalid pattern: Minor variable %m% used more than once in pattern");
		}

		if (checkPattern.matches("\\S*%b%\\S*%b%\\S*"))
		{
			throw new IllegalArgumentException(
				"Invalid pattern: Build variable %b% used more than once in pattern");
		}

		if (checkPattern.matches("\\S*%d%\\S*%d%\\S*"))
		{
			throw new IllegalArgumentException(
				"Invalid pattern: Date variable %d% used more than once in pattern");
		}

		if (checkPattern.matches("\\S*%t%\\S*%t%\\S*"))
		{
			throw new IllegalArgumentException(
				"Invalid pattern: Time variable %t% used more than once in pattern");
		}

		//
		// Validate the escape/variable syntax is used correctly and set the usage booleans
		//
		int index = 0;

		while (index >= 0)
		{

			//
			// Find the next pattern block to validate
			//
			index = checkPattern.indexOf("%", index);

			if (index >= 0)
			{

				//
				// The string must have enough space left for either another % or a variable
				// followed by a %
				//
				if (checkPattern.length() == (index + 1))
				{
					throw new IllegalArgumentException(
						"Invalid pattern: unbalanced % found at end of pattern");
				}

				if ((checkPattern.length() == (index + 2))
					&& (checkPattern.charAt(index + 1) != '%'))
				{
					throw new IllegalArgumentException(
						"Invalid pattern: unbalanced % found at end of pattern");
				}

				final char nextChar = checkPattern.charAt(index + 1);

				if (nextChar == '%')
				{
					index += 2;
				}
				else
				{
					final char fenceChar = checkPattern.charAt(index + 2);

					if (fenceChar != '%')
					{
						throw new IllegalArgumentException(
							"Invalid pattern: invalid variable reference at pattern index "
								+ (index + 2));
					}

					switch (nextChar)
					{
					case 'M':
						setUseMajor(true);

						break;

					case 'm':
						setUseMinor(true);

						break;

					case 'b':
						setUseBuild(true);

						break;

					case 'd':
					case 't':
						break;

					default:
						throw new IllegalArgumentException(
							"Invalid pattern: invalid variable reference '" + nextChar
								+ "' at pattern index " + (index + 1));
					}

					index += 3;
				}
			}
		}

		return checkPattern;
	}

	private String generateVersionString()
	{
		final StringBuilder versionStr = new StringBuilder();
		final String buildPattern = getPattern();

		//
		// The pattern is known valid, just fill in the blanks
		//
		int index = 0;
		int lastIndex = index;

		while (index >= 0)
		{
			//
			// Find the next pattern block to validate
			//
			index = buildPattern.indexOf("%", index);

			int nextIndex = index + 3;

			if (index >= 0)
			{
				//
				// Place the block from the last match to the current into the string
				//
				versionStr.append(buildPattern.substring(lastIndex, index));

				final char nextChar = buildPattern.charAt(index + 1);

				switch (nextChar)
				{
				case '%':
					versionStr.append("%");

					//
					// Backup the index one since this is only 2 characters consumed
					//
					nextIndex -= 1;

					break;

				case 'M':
					versionStr.append(getMajor());

					break;

				case 'm':
					versionStr.append(getMinor());

					break;

				case 'b':
					versionStr.append(getBuild());

					break;

				case 'd':
					final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_PATTERN);
					dateFormatter.setTimeZone(getVersionTimeZone());

					versionStr.append(dateFormatter.format(getBuildDate()));

					break;

				case 't':
					final SimpleDateFormat timeFormatter = new SimpleDateFormat(TIME_FORMAT_PATTERN);
					timeFormatter.setTimeZone(getVersionTimeZone());

					versionStr.append(timeFormatter.format(getBuildDate()));

					break;

				default:
					//
					// This state is not possible if the validate method works. Not testable
					// without breaking private contract
					//
					throw new IllegalStateException("Invalid pattern detected '" + getPattern()
						+ "' at index: " + index);
				}

				index = nextIndex;
				lastIndex = index;
			}
		}

		//
		// Tack on the postfix (if any)
		//
		versionStr.append(buildPattern.substring(lastIndex));

		return versionStr.toString();
	}

	private void setUseLocalTimeZone(final boolean useLocalTimeZone)
	{
		this.useLocalTimeZone = useLocalTimeZone;
	}

	private TimeZone getVersionTimeZone()
	{
		TimeZone timeZone;

		if (useLocalTimeZone)
		{
			timeZone = TimeZone.getDefault();
		}
		else
		{
			timeZone = TimeZone.getTimeZone("UTC");
		}
		return timeZone;
	}

	public int getBuild()
	{
		return build;
	}

	public Date getBuildDate()
	{
		return buildDate;
	}

	public int getMajor()
	{
		return major;
	}

	public int getMinor()
	{
		return minor;
	}

	private int getNextNonNumberIndex(final String candidate, final int startIndex)
	{
		int currentIndex = startIndex;

		while ((currentIndex < candidate.length())
			&& Character.isDigit(candidate.charAt(currentIndex)))
		{
			currentIndex++;
		}

		return currentIndex;
	}

	private int getNextNumberIndex(final String candidate, final int startIndex)
	{
		int currentIndex = startIndex;

		while ((currentIndex < candidate.length())
			&& !Character.isDigit(candidate.charAt(currentIndex)))
		{
			currentIndex++;
		}

		return currentIndex;
	}

	public String getPattern()
	{
		return pattern;
	}

	public String getValidatePattern()
	{
		return validatePattern;
	}

	public void incrementBuild()
	{
		build++;
		updateDate();
	}

	public void incrementMajor()
	{
		major++;
		setMinor(0);
		updateDate();
	}

	public void incrementMinor()
	{
		minor++;
		updateDate();
	}

	//
	// This method increments the build version in the most 'natural' way.
	// The build number is considered the most volatile, followed by the minor
	// version and finally followed by the major version. The date is always updated
	// as a result of the increment of the build version.
	//
	public void incrementVersion()
	{
		if (useBuild())
		{
			incrementBuild();
		}
		else if (useMinor())
		{
			incrementMinor();
		}
		else if (useMajor())
		{
			incrementMajor();
		}
		else
		{
			updateDate();
		}
	}

	@SuppressWarnings("hiding")
	private void init(final int major, final int minor, final int build, final Date buildDate,
		final boolean useLocalTimeZone)
	{
		setMajor(major);
		setMinor(minor);
		setBuild(build);
		setBuildDate(buildDate);
		setUseLocalTimeZone(useLocalTimeZone);
	}

	private void parseCandidate(final String candidate)
		throws ParseException
	{
		if (null != candidate)
		{

			//
			// These values are extracted during parse and pulled together to make a
			// valid date at the end of the parse.
			//
			String dateStr = "";
			String timeStr = "";

			//
			// Note that the build date can only be derived if the 'date' portion of the
			// pattern is set. The derived date if that date pattern is not present will be
			// 'now'. If the time pattern variable is not present, the date will be midnight
			// of the date in the candidate.
			//
			final String parsePattern = getPattern();

			//
			// The pattern is known valid, just fill in the blanks
			//
			int patternIndex = 0;
			int candidateIndex = getNextNumberIndex(candidate, 0);

			while (patternIndex >= 0)
			{

				//
				// Find the next pattern block to validate
				//
				patternIndex = parsePattern.indexOf("%", patternIndex);

				int nextPatternIndex = patternIndex + 3;
				int nextCandidateIndex = candidateIndex;

				if (patternIndex >= 0)
				{
					final char nextChar = parsePattern.charAt(patternIndex + 1);

					switch (nextChar)
					{
					case '%':

						//
						// backup the index one since this is only 2 characters consumed
						//
						nextPatternIndex -= 1;

						break;

					case 'M':
						nextCandidateIndex = getNextNonNumberIndex(candidate, candidateIndex);

						if (candidateIndex != nextCandidateIndex)
						{
							setMajor(Integer.valueOf(candidate.substring(candidateIndex,
								nextCandidateIndex)));
						}
						else
						{
							throw new ParseException("Unable to match %M% for pattern '"
								+ parsePattern + "'", patternIndex);
						}

						break;

					case 'm':
						nextCandidateIndex = getNextNonNumberIndex(candidate, candidateIndex);

						if (candidateIndex != nextCandidateIndex)
						{
							setMinor(Integer.valueOf(candidate.substring(candidateIndex,
								nextCandidateIndex)));
						}
						else
						{
							throw new ParseException("Unable to match %m% for pattern '"
								+ parsePattern + "'", patternIndex);
						}

						break;

					case 'b':
						nextCandidateIndex = getNextNonNumberIndex(candidate, candidateIndex);

						if (candidateIndex != nextCandidateIndex)
						{
							setBuild(Integer.valueOf(candidate.substring(candidateIndex,
								nextCandidateIndex)));
						}
						else
						{
							throw new ParseException("Unable to match %b% for pattern '"
								+ parsePattern + "'", patternIndex);
						}

						break;

					case 'd':
						nextCandidateIndex = getNextNonNumberIndex(candidate, candidateIndex);

						if (candidateIndex != nextCandidateIndex)
						{
							dateStr = candidate.substring(candidateIndex, nextCandidateIndex);
						}
						else
						{
							throw new ParseException("Unable to match %d% for pattern '"
								+ parsePattern + "'", patternIndex);
						}

						break;

					case 't':
						nextCandidateIndex = getNextNonNumberIndex(candidate, candidateIndex);

						if (candidateIndex != nextCandidateIndex)
						{
							timeStr = candidate.substring(candidateIndex, nextCandidateIndex);
						}
						else
						{
							throw new ParseException("Unable to match %t% for pattern '"
								+ parsePattern + "'", patternIndex);
						}

						break;

					default:

						//
						// This state is not possible if the validate method works. Not testable
						// without breaking private contract
						//
						throw new IllegalStateException("Invalid pattern detected '" + getPattern()
							+ "' at index: " + patternIndex);
					}

					patternIndex = nextPatternIndex;
					candidateIndex = getNextNumberIndex(candidate, nextCandidateIndex);
				}
			}

			//
			// Last step is to formulate the date if possible
			//
			if (dateStr.length() > 0)
			{
				String format = DATE_FORMAT_PATTERN;
				String toParse = dateStr;

				if (timeStr.length() > 0)
				{
					format = DATE_FORMAT_PATTERN + "." + TIME_FORMAT_PATTERN;
					toParse = dateStr + "." + timeStr;
				}

				final SimpleDateFormat formatter = new SimpleDateFormat(format);
				formatter.setTimeZone(getVersionTimeZone());
				formatter.setLenient(true);

				try
				{
					setBuildDate(formatter.parse(toParse));
				}
				catch (final ParseException e)
				{
					throw new ParseException("Unable to match date for pattern '" + parsePattern
						+ "'", 0);
				}
			}
		}
	}

	public void setBuild(final int build)
	{
		this.build = build;
	}

	public void setBuildDate(final Date buildDate)
	{
		this.buildDate = buildDate;

		if (null == buildDate)
		{
			this.buildDate = new Date();
		}
	}

	public void setMajor(final int major)
	{
		this.major = major;
	}

	public void setMinor(final int minor)
	{
		this.minor = minor;
	}

	public void setPattern(final String newPattern)
	{
		setPattern(newPattern, null);
	}

	public void setPattern(String newPattern, final String newValidatePattern)
	{
		if (null == newPattern)
		{
			newPattern = DEFAULT_PATTERN;
		}

		pattern = checkPattern(newPattern);
		validatePattern = newValidatePattern;

		if (null == newValidatePattern)
		{
			validatePattern = generateValidatePattern(getPattern());
		}
	}

	private void setUseBuild(final boolean useBuild)
	{
		this.useBuild = useBuild;
	}

	private void setUseMajor(final boolean useMajor)
	{
		this.useMajor = useMajor;
	}

	private void setUseMinor(final boolean useMinor)
	{
		this.useMinor = useMinor;
	}

	@Override
	public String toString()
	{
		final String versionString = generateVersionString();

		if (!versionString.matches(getValidatePattern()))
		{
			throw new IllegalStateException("Version string generated '" + versionString
				+ "' from pattern '" + getPattern() + "' does not match candidate pattern '"
				+ getValidatePattern() + "'.  Output and candidate patterns must be consistent");
		}

		return versionString;
	}

	public void updateDate()
	{
		setBuildDate(new Date());
	}

	public void updateMajor(final int newMajor)
	{
		if (getMajor() != newMajor)
		{
			setMajor(newMajor);
			setMinor(0);
		}
	}

	public boolean useBuild()
	{
		return useBuild;
	}

	public boolean useMajor()
	{
		return useMajor;
	}

	public boolean useMinor()
	{
		return useMinor;
	}
}
