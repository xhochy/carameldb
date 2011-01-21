package com.xhochy.carameldb;


import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;


/**
 * Runner that resets the database before each test.
 */
public class CaramelRunner extends BlockJUnit4ClassRunner {

    /**
     * Create a new instance. This method should be only called by JUnit.
     * @param klass The class to be tested.
     * @throws InitializationError Error on initialization.
     */
    public CaramelRunner(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new DatabaseInvokeMethod(method, test);
    }
}
