package org.kercheval.gradle.vcs;

import java.io.File;

import org.gradle.api.logging.Logger;
import org.kercheval.gradle.vcs.git.VCSGitImpl;
import org.kercheval.gradle.vcs.none.VCSNoneImpl;

//
// This is a simple factory class that supports the return of a vcs access
// layer.
//
// NOTE: Currently there is only GIT support, but auto detection of any
// number of other revision control sources is very reasonable.
//
public class VCSAccessFactory
{
	public static VCSAccess getCurrentVCS(final String type, final File srcRootDir,
		final Logger logger)
	{
		final VCSAccess rVal = new VCSNoneImpl(srcRootDir, logger);
		final String desiredType = type.toLowerCase();
		if (desiredType.equalsIgnoreCase(VCSAccess.Type.GIT.toString()))
		{
			return new VCSGitImpl(srcRootDir, logger);
		}
		return rVal;
	}
}
