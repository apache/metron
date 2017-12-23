#  How To Contribute
As an open source project, Metron welcomes contributions of all forms. There are several great ways to contribute!

* [Contributing a Code Change](#contributing-a-code-change)
* Reviewing pull requests on our GitHub page. Check out current open [Pull Requests](https://github.com/apache/metron/pulls)
* Improve our documentation. Our docs are self contained in the project in README files. Doc changes are the same process as a code change. See [Contributing a Code Change](#contributing-a-code-change)
* Contributing to or starting discussions on the mailing lists. Both the user and dev lists are great places to give and receive help, or provide feedback. See [Mailing Lists](http://metron.apache.org/community/#mailinglist)
* Filing tickets for features, enhancements, and bugs to our JIRA. Take a look at [Reporting Issues](https://cwiki.apache.org/confluence/display/METRON/Reporting+Issues) and the [Metron JIRA](https://issues.apache.org/jira/projects/METRON)

##  Contributing A Code Change
1. Open a [JIRA ticket](https://issues.apache.org/jira/projects/METRON) associated with your change, if one doesn't already exist. Assign it to yourself and give a good description.
    * Feel free to ask questions on the lists and collaborate! 
1. Implement your change
    * We recommend setting up [Travis CI](https://docs.travis-ci.com) on your personal Github repo to handle long running testing. If the Travis build fails, you'll want to look into it. See [Getting started](https://docs.travis-ci.com/user/getting-started/#To-get-started-with-Travis-CI) for instructions.
1. Open a GitHub [Pull Request](https://github.com/apache/metron/pulls) with your change
    * Fork the Metron repo. Look at [Fork a repo](https://help.github.com/articles/fork-a-repo/)
    * Make the PR. See [Creating a pull request from a fork](https://help.github.com/articles/creating-a-pull-request-from-a-fork/)
    * Make sure the PR name starts with your JIRA ticket number (METRON-XXXX).
1. Iterate on your change with reviewers until it's merged into master.

## Development Guidelines
The full guidelines can be found on the [Metron wiki](https://cwiki.apache.org/confluence/display/METRON/Development+Guidelines).  They boil down to
1. Make sure you've tested your change.
1. Make sure you've documented your change.
1. Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
    * If the file is a different style follow that style.
1. Be open to feedback.