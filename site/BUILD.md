# Apache ORC docs site

This directory contains the code for the Apache ORC web site,
[orc.apache.org](https://orc.apache.org/).

## Setup

1. `cd site`
2. `git clone https://git-wip-us.apache.org/repos/asf/orc.git -b asf-site target`
3. `sudo gem install bundler`
4. `sudo gem install github-pages jeykll`
4. `bundle install`

## Running locally

You can preview your contributions before opening a pull request by running from within the directory:

1. `bundle exec jekyll serve`

## Pushing to site

1. `cd site/target`
2. `git status`
3. You'll need to `git add` any new files
4. `git commit -a`
5. `git push origin asf-site`