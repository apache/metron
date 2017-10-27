# bundles-testing

Bundles testing is a project for testing the basic functionality of the bundle-lib and bundles.
The idea is that we have an integration test module (bundles-test-integration), that deplends on the interface module
(bundles-test-integration) but not the implementation.

The implementation (bundles-test-implemenation) is packaged as a bundle (bundles-test-bundle).

The integration test loads the implemenation through the bundle system, by interface, and executes its methods.
The demonstrates the main use case.

