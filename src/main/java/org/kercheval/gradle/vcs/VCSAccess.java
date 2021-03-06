package org.kercheval.gradle.vcs;

import java.util.List;

import org.kercheval.gradle.info.InfoSource;
import org.kercheval.gradle.info.SortedProperties;

//
// This interface supports the specific types of operation required by
// programmatic access to the VCS system in use.
//
// NOTE: GIT is the only supported system at the moment.
//
public interface VCSAccess
	extends InfoSource
{
	//
	// This type refers to the VCS type in use for an access object.
	// Currently only GIT is implemented, but Mercurial, Perforce and
	// SVN are all likely candidates.
	//
	public enum Type
	{
		NONE,
		GIT,
		MERCURIAL
	}

	//
	// Create a new branch in the current system. If ignoreOrigin is true
	// the branch will be created/verified on the local repository. If
	// ignoreOrigin is false, the branch will be first checked on the origin
	// and pulled if present on the origin and not on the current local. If
	// a local branch is created because the origin is not present, it will
	// be pushed to origin.
	//
	public void createBranch(final String branchName, final String remoteOrigin,
		final boolean ignoreOrigin)
		throws VCSException;

	//
	// Write a tag into the repository
	//
	public void createTag(final VCSTag tag)
		throws VCSException;

	//
	// Obtain from the branch and origin the current content for that branch.
	//
	public void fetch(final String remoteOrigin)
		throws VCSException;

	//
	// Get tags from repository.
	//
	public List<VCSTag> getAllTags()
		throws VCSException;

	//
	// Obtain the current branch name
	//
	public String getBranchName()
		throws VCSException;

	//
	// Obtain 'interesting' information about the current VCS usage
	// and return that as property information.
	//
	@Override
	public SortedProperties getInfo();

	//
	// Return the current status of the VCS system (all workspace changes)
	//
	public VCSStatus getStatus()
		throws VCSException;

	public List<VCSTag> getTags(final String regexFilter)
		throws VCSException;

	//
	// Get the current VCSType.
	//
	public Type getType();

	//
	// Merge one branch into another. This will fail if there are
	// any conflicts or merge changes required. To succeed the branch
	// update must be the equivalent of a git fast-forward merge.
	// Failure will throw a VCSException. If remoteOrigin is null
	// the merge will be from the local branch 'fromBranch'. If
	// the boolean fastForwardOnly is true, the merge will only occur
	// if there are no changes on the origin that are not also on the
	// fromBranch.
	//
	public void merge(final String fromBranch, String remoteOrigin, boolean fastForwardOnly)
		throws VCSException;

	//
	// Push the a branch or tag back into the origin. Push failures will result in
	// an exception being thrown. If pushTag is true, the from parameter is
	// treated as a tag rather than a branch.
	//
	public void push(final String from, final String remoteOrigin, boolean pushTag)
		throws VCSException;
}
