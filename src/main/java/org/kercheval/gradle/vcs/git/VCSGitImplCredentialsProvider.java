package org.kercheval.gradle.vcs.git;

import java.util.Map;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.StringUtils;
import org.kercheval.gradle.console.ConsoleException;
import org.kercheval.gradle.console.TextDevice;

public class VCSGitImplCredentialsProvider
	extends CredentialsProvider
{
	//
	// These can be set to supply default usernames and passwords for GIT interactions
	//
	public static final String GIT_ORIGIN_USERNAME = "GIT_ORIGIN_USERNAME";
	public static final String GIT_ORIGIN_PASSWORD = "GIT_ORIGIN_PASSWORD";

	//
	// Default username and password (from contructor init)
	//
	private final String username;
	private final String password;

	//
	// The console IO device
	//
	private final TextDevice userIO;

	//
	// The environment to acquire variables from. Done using statics to allow testing
	// by adding to the map
	//
	static private Map<String, String> environmentMap = System.getenv();

	static protected void setEnvironmentMap(final Map<String, String> newMap)
	{
		environmentMap = newMap;
	}

	public VCSGitImplCredentialsProvider(final TextDevice userIO)
	{
		//
		// Obtain the default values from the environment
		//
		this(userIO, environmentMap.get(GIT_ORIGIN_USERNAME), environmentMap
			.get(GIT_ORIGIN_PASSWORD));
	}

	public VCSGitImplCredentialsProvider(final TextDevice userIO,
		final String username,
		final String password)
	{
		this.userIO = userIO;
		this.username = username;
		this.password = password;
	}

	//
	// The prompts always show the requesting URI
	//
	private void displayPrompt(final URIish uri, final CredentialItem credentialItem)
		throws ConsoleException
	{
		userIO.printf(uri.toASCIIString() + " - " + credentialItem.getPromptText() + ": ");
	}

	//
	// The yesno prompt should always include the allowable responses
	//
	private void displayYNPrompt(final URIish uri, final CredentialItem credentialItem)
		throws ConsoleException
	{
		userIO.printf(uri.toASCIIString() + " - " + credentialItem.getPromptText() + " [y/n]: ");
	}

	@Override
	public boolean get(final URIish uri, final CredentialItem... items)
		throws UnsupportedCredentialItem
	{
		for (final CredentialItem item : items)
		{
			//
			// Supply our default user name
			//
			if ((item instanceof CredentialItem.Username) && !StringUtils.isEmptyOrNull(username))
			{
				((CredentialItem.Username) item).setValue(username);
				continue;
			}

			//
			// Password or secure credential item gets our default password
			//
			if (item instanceof CredentialItem.Password)
			{
				final CredentialItem.Password credentialItem = (CredentialItem.Password) item;
				if (!StringUtils.isEmptyOrNull(password))
				{
					credentialItem.setValue(password.toCharArray());
				}
				else
				{
					displayPrompt(uri, credentialItem);
					credentialItem.setValue(userIO.readPassword());
				}
				continue;
			}

			//
			// Generic string type
			//
			if (item instanceof CredentialItem.StringType)
			{
				final CredentialItem.StringType credentialItem = (CredentialItem.StringType) item;
				if (credentialItem.isValueSecure())
				{
					if (!StringUtils.isEmptyOrNull(password))
					{
						credentialItem.setValue(String.valueOf(password.toCharArray()));
					}
					else
					{
						displayPrompt(uri, credentialItem);
						credentialItem.setValue(String.valueOf(userIO.readPassword()));
					}
				}
				else
				{
					displayPrompt(uri, credentialItem);
					credentialItem.setValue(userIO.readLine());
				}
				continue;
			}

			//
			// Generic char array type
			//
			if (item instanceof CredentialItem.CharArrayType)
			{
				final CredentialItem.CharArrayType credentialItem = (CredentialItem.CharArrayType) item;
				if (credentialItem.isValueSecure())
				{
					if (!StringUtils.isEmptyOrNull(password))
					{
						credentialItem.setValue(password.toCharArray());
					}
					else
					{
						displayPrompt(uri, credentialItem);
						credentialItem.setValue(userIO.readPassword());
					}
				}
				else
				{
					displayPrompt(uri, credentialItem);
					credentialItem.setValue(userIO.readLine().toCharArray());
				}
				continue;
			}

			//
			// JGit is trying to send a message
			//
			if (item instanceof CredentialItem.InformationalMessage)
			{
				final CredentialItem.InformationalMessage credentialItem = (CredentialItem.InformationalMessage) item;
				displayPrompt(uri, credentialItem);
				continue;
			}

			//
			// Need to query the user (usually an SSH host query)
			//
			if (item instanceof CredentialItem.YesNoType)
			{
				final CredentialItem.YesNoType credentialItem = (CredentialItem.YesNoType) item;
				displayYNPrompt(uri, credentialItem);
				final String response = userIO.readLine();
				credentialItem.setValue(response.startsWith("y") || response.startsWith("Y"));
				continue;
			}

			//
			// Unsupported type
			//
			throw new UnsupportedCredentialItem(uri, item.getClass().getName() + ":"
				+ item.getPromptText());
		}
		return true;
	}

	@Override
	public boolean isInteractive()
	{
		return true;
	}

	@Override
	public boolean supports(final CredentialItem... items)
	{
		for (final CredentialItem i : items)
		{
			if (i instanceof CredentialItem.StringType)
			{
				continue;
			}
			else if (i instanceof CredentialItem.CharArrayType)
			{
				continue;
			}
			else if (i instanceof CredentialItem.InformationalMessage)
			{
				continue;
			}
			else if (i instanceof CredentialItem.YesNoType)
			{
				continue;
			}
			else
			{
				return false;
			}
		}
		return true;
	}
}
