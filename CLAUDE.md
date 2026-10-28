
<!-- local-build-link -->
> **Local build setup:** see [iDempiereCLDE/LOCAL_BUILD.md](../iDempiereCLDE/LOCAL_BUILD.md) for the one-time `~/.m2/settings.xml` configuration
> that lets `mvn verify` resolve P2 artifacts from sibling local builds instead of S3.

- put tests into com.cloudempiere.ai.test bundle (local test bundle)
- use @UnitTest, @IntegrationTest, @E2ETest annotations from com.cloudempiere.ai.test.categories
- use bash script for testing: ./run-unit-tests.sh (unit), ./run-unit-tests.sh --integration (integration)
- iDempiere v14, Java 17: build with `JAVA_HOME` set to Corretto 17 (the default may still be Java 11)
- the developer does not use Eclipse: run `mvn compile` before `./run-unit-tests.sh` when main code changed, since the runner uses `target/classes`
- debug log level must be at least warning so the developer can see it in the server console
