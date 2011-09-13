package org.eclipse.birt.report.data.oda.mongodb.i18n;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Resource messages wrapper for the package to obtain localized message text.
 */

public class Messages
{
    private static final String BUNDLE_NAME = "org.eclipse.birt.report.data.oda.mongodb.i18n.messages";//$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME );

    private Messages()
    {
    }

    public static String getString( String key )
    {
        try
        {
            return RESOURCE_BUNDLE.getString( key );
        }
        catch( MissingResourceException e )
        {
            return '!' + key + '!';
        }
    }
}