Thank you for submitting a contribution to Apache Metron (Incubating).
Please refer to our [Development Guidelines](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=61332235) for the complete guide to follow for contributions.
Please refer also to our [Build Verification guildlines](https://cwiki.apache.org/confluence/display/METRON/Verifying+Builds?show-miniview) for complete smoke testing guides.


In order to streamline the review of the contribution we ask you follow these guidelines and ask you to double check
the following:

### For all changes:
- [ ] Is there a JIRA ticket associated with this PR? If not one needs to be created at [Metron Jira](https://issues.apache.org/jira/browse/METRON/?selectedTab=com.atlassian.jira.jira-projects-plugin:summary-panel). 
- [ ] Does your PR title start with METRON-XXXX where XXXX is the JIRA number you are trying to resolve? Pay particular attention to the hyphen "-" character.
- [ ] Has your PR been rebased against the latest commit within the target branch (typically master)?


### For code changes:
- [ ] Have you included steps to reproduce the behavior or problem that is being changed or addressed?
- [ ] Have you included steps or a guide to how the change may be verified and tested manually?
- [ ] Have you ensured that the full suite of tests and checks have been executed in the root incubating-metron folder via:

```
mvn -q clean integration-test install && build_utils/verify_licenses.sh 
```

- [ ] Have you written or updated unit tests and or integration tests to verify your changes?
- [ ] If adding new dependencies to the code, are these dependencies licensed in a way that is compatible for inclusion under [ASF 2.0](http://www.apache.org/legal/resolved.html#category-a)? 
- [ ] Have you verified the basic functionality of the build by building and running locally with Vagrant full-dev environment or the equivalent?

### For documentation related changes:
- [ ] Have you ensured that format looks appropriate for the output in which it is rendered by building and verifying the site-book? If not then run the following commands and the verify changes via site-book/target/site/index.html.

```
cd site-book
bin/generate-md.sh
mvn site:site

```

### Note:
Please ensure that once the PR is submitted, you check travis-ci for build issues and submit an update to your PR as soon as possible.
It is also recommened that [travis-ci](https://travis-ci.org) is set up for your personal repository such that your branches are built there before submitting a pull request.
