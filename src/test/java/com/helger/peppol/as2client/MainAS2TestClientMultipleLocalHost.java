/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.as2client;

import java.io.File;
import java.net.URI;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.client.AS2ClientResponse;
import com.helger.as2lib.crypto.ECryptoAlgorithmSign;
import com.helger.bdve.executorset.VESID;
import com.helger.bdve.peppol.PeppolValidation360;
import com.helger.bdve.result.ValidationResult;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.timing.StopWatch;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpclient.SMPClientConfiguration;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.peppol.url.IPeppolURLProvider;
import com.helger.peppol.url.PeppolURLProvider;
import com.helger.security.keystore.EKeyStoreType;

/**
 * Main class to send AS2 messages.
 *
 * @author Philip Helger
 */
public final class MainAS2TestClientMultipleLocalHost
{
  /** The file path to the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PATH = "as2-client-data/client-certs.p12";
  /** The password to open the PKCS12 key store */
  private static final String PKCS12_CERTSTORE_PASSWORD = "peppol";
  /** Your AS2 sender ID */
  private static final String SENDER_AS2_ID = "APP_1000000101";
  /** Your AS2 sender email address */
  private static final String SENDER_EMAIL = "peppol@example.org";
  /** Your AS2 key alias in the PKCS12 key store */
  private static final String SENDER_KEY_ALIAS = SENDER_AS2_ID;
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;
  private static final IPeppolURLProvider URL_PROVIDER = PeppolURLProvider.INSTANCE;
  /** The PEPPOL sender participant ID */
  private static final IParticipantIdentifier SENDER_PEPPOL_ID = IF.createParticipantIdentifierWithDefaultScheme ("9999:test-sender");

  private static final Logger s_aLogger = LoggerFactory.getLogger (MainAS2TestClientMultipleLocalHost.class);

  static
  {
    // Set Proxy Settings from property file.
    SMPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();

    // Enable or disable debug mode
    GlobalDebug.setDebugModeDirect (false);
  }

  @SuppressWarnings ({ "null" })
  public static void main (final String [] args) throws Exception
  {
    /** The PEPPOL document type to use. */
    final IDocumentTypeIdentifier aDocTypeID = EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS4A_V20.getAsDocumentTypeIdentifier ();
    /** The PEPPOL process to use. */
    final IProcessIdentifier aProcessID = EPredefinedProcessIdentifier.BIS4A_V2.getAsProcessIdentifier ();
    IParticipantIdentifier aReceiver = null;
    String sTestFilename = null;
    IReadableResource aTestResource = null;
    String sReceiverID = null;
    String sReceiverKeyAlias = null;
    String sReceiverAddress = null;
    ISMLInfo aSML = ESML.DIGIT_PRODUCTION;
    final VESID aValidationKey = true ? null : PeppolValidation360.VID_OPENPEPPOL_T10_V2;
    final URI aSMPURI = null;
    final ECryptoAlgorithmSign eMICAlg = ECryptoAlgorithmSign.DIGEST_SHA_1;
    final HttpHost aProxy = SMPClientConfiguration.getHttpProxy ();

    if (true)
    {
      // localhost test endpoint
      aReceiver = IF.createParticipantIdentifierWithDefaultScheme ("9915:test");
      sTestFilename = "xml/as2-test-at-gov.xml";
      // Avoid SMP lookup
      sReceiverAddress = "http://localhost:8080/as2";
      sReceiverID = SENDER_AS2_ID;
      sReceiverKeyAlias = SENDER_KEY_ALIAS;
      aSML = ESML.DIGIT_TEST;
    }

    if (aTestResource == null && sTestFilename != null)
      aTestResource = new ClassPathResource (sTestFilename);

    final SMPClientReadOnly aSMPClient = aSMPURI != null ? new SMPClientReadOnly (aSMPURI)
                                                         : new SMPClientReadOnly (URL_PROVIDER, aReceiver, aSML);

    // No proxy for local host
    if (!aSMPClient.getSMPHostURI ().startsWith ("http://localhost") &&
        !aSMPClient.getSMPHostURI ().startsWith ("http://127."))
    {
      aSMPClient.setProxy (aProxy);
    }

    final int nCount = 1_000;
    final StopWatch aSC = StopWatch.createdStarted ();
    for (int i = 0; i < nCount; ++i)
      try
      {
        final AS2ClientResponse aResponse = new AS2ClientBuilder ().setSMPClient (aSMPClient)
                                                                   .setKeyStore (EKeyStoreType.PKCS12,
                                                                                 new File (PKCS12_CERTSTORE_PATH),
                                                                                 PKCS12_CERTSTORE_PASSWORD)
                                                                   .setSaveKeyStoreChangesToFile (false)
                                                                   .setSenderAS2ID (SENDER_AS2_ID)
                                                                   .setSenderAS2Email (SENDER_EMAIL)
                                                                   .setSenderAS2KeyAlias (SENDER_KEY_ALIAS)
                                                                   .setReceiverAS2ID (sReceiverID)
                                                                   .setReceiverAS2KeyAlias (sReceiverKeyAlias)
                                                                   .setReceiverAS2Url (sReceiverAddress)
                                                                   .setAS2SigningAlgorithm (eMICAlg)
                                                                   .setBusinessDocument (aTestResource)
                                                                   .setPeppolSenderID (SENDER_PEPPOL_ID)
                                                                   .setPeppolReceiverID (aReceiver)
                                                                   .setPeppolDocumentTypeID (aDocTypeID)
                                                                   .setPeppolProcessID (aProcessID)
                                                                   .setValidationKey (aValidationKey)
                                                                   .sendSynchronous ();
        if (aResponse.hasException ())
          s_aLogger.warn (aResponse.getAsString ());

        s_aLogger.info ("Done " + i);
      }
      catch (final AS2ClientBuilderValidationException ex)
      {
        for (final ValidationResult aVR : ex.getValidationResult ())
          if (aVR.isFailure ())
            s_aLogger.error (aVR.toString ());
          else
            s_aLogger.info (aVR.toString ());
      }
      catch (final AS2ClientBuilderException ex)
      {
        ex.printStackTrace ();
      }
    aSC.stop ();
    s_aLogger.info ("Sending " +
                    nCount +
                    " docs took " +
                    aSC.getSeconds () +
                    "secs which is " +
                    aSC.getMillis () / nCount +
                    " msec per document");
  }
}
