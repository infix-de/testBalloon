## Upgrading from TestBalloon 0.7.x to {{ project.version }}

### Android host-side (unit) tests

1. Remove the `-jvm` suffix from each dependency `"de.infix.testBalloon:testBalloon-framework-core-jvm:$testBalloonVersion"`.
2. Add a dependency on JUnit 4: `"junit:junit:4.13.2"`

### Custom test functions

#### Replace with test-level fixtures (optional)

=== "TestBalloon 0.7.x"

    ```kotlin
    testSuite("Multiple tests with fresh state") {
        @TestRegistering 
        fun test(name: String, action: suspend Service.() -> Unit) = 
            this.test(name) {
                val service = Service().apply {
                    signIn(userName = "siobhan", password = "ask") 
                }
                service.action() 
                signOut()
            }
    
        test("deposit") {
            deposit(Amount(20.0)) 
            assertEquals(Amount(40.99), accountBalance()) 
        }
    
        test("withdraw") {
            withdraw(Amount(20.0))
            assertEquals(Amount(0.99), accountBalance())
        }
    }
    ```

=== "TestBalloon {{ project.version }}"

    ```kotlin
    testSuite("Multiple tests with fresh state") {
        testFixture {
            Service().apply {
                signIn(userName = "siobhan", password = "ask") 
            } 
        } closeWith {
            signOut()
        } asContextForEach {
            test("deposit") {
                deposit(Amount(20.0))
                assertEquals(Amount(40.99), accountBalance())
            }
    
            test("withdraw") {
                withdraw(Amount(20.0))
                assertEquals(Amount(0.99), accountBalance())
            }
        }
    }
    ```

#### Use `TestSuiteScope` (optional)

Change extension functions like `TestSuite.myCustomTest()` to `TestSuiteScope.myCustomTest()`. This makes them compatible with the new fixture scopes and other custom scopes.

### `TestConfig` changes 

* Change `TestInvocation.SEQUENTIAL` to `TestConfig.Invocation.Sequential`.
* Change `TestInvocation.CONCURRENT` to `TestConfig.Invocation.Concurrent`.
* Change `TestExecutionTraversal` to `TestConfig.ExecutionTraversal`.
* Change `TestExecutionScope` to `Test.ExecutionScope`.

### Other changes

* Change `TestSuite.Fixture` to `TestFixture`.
* Change `TestElementEvent` to `TestElement.Event`.
* Change `TestPermit` to `TestConfig.Permit`.
* Change members of `TestPlatform.Type` from _SCREAMING_SNAKE_CASE_ to _PascalCase_.
