#!/bin/bash

rm -rf SRPMS/ RPMS/ && \
rpmbuild -v -ba --define "_topdir $(pwd)" --define "_version 0.2.0BETA" SPECS/metron.spec && \
rpmlint -i SPECS/metron.spec RPMS/*/metron* SRPMS/metron

