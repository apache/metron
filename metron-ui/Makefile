testcmd=./node_modules/istanbul/lib/cli.js cover \
	./node_modules/mocha/bin/_mocha -- --check-leaks -R spec

testwatchcmd=./node_modules/istanbul/lib/cli.js cover \
	./node_modules/mocha/bin/_mocha -- --check-leaks --watch -R spec

test: test-all

test-watch:
ifeq ($(IN_TRAVIS),true)
	PORT=4000 NODE_ENV=ci $(testwatchcmd)
else
	PORT=4000 NODE_ENV=test $(testwatchcmd)
endif

test-all:
ifeq ($(IN_TRAVIS),true)
	PORT=4000 NODE_ENV=ci $(testcmd)
else
	PORT=4000 NODE_ENV=test $(testcmd)
endif

# Load test data into DB
seed:
	node script/es_fetch.js && script/es_seed.sh

clean:
	rm -rf ./node_modules ./coverage

