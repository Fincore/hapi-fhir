package ca.uhn.fhir.jpa.provider.dstu21;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hl7.fhir.dstu21.model.DecimalType;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.dstu21.model.Questionnaire;
import org.hl7.fhir.dstu21.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.dstu21.model.QuestionnaireResponse;
import org.hl7.fhir.dstu21.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseValidatingInterceptor;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;

public class ResourceProviderQuestionnaireResponseDstu21Test extends BaseResourceProviderDstu21Test {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ResourceProviderQuestionnaireResponseDstu21Test.class);
	private static RequestValidatingInterceptor ourValidatingInterceptor;

	@Override
	@Before
	public void before() throws Exception {
		super.before();

		if (ourValidatingInterceptor == null) {
			ourValidatingInterceptor = new RequestValidatingInterceptor();
			ourValidatingInterceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
			for (IValidatorModule next : myAppCtx.getBeansOfType(IValidatorModule.class).values()) {
				ourValidatingInterceptor.addValidatorModule(next);
			}
			ourRestServer.registerInterceptor(ourValidatingInterceptor);
		}
	}

	@AfterClass
	public static void afterClass() {
		ourRestServer.unregisterInterceptor(ourValidatingInterceptor);
	}
	
	
	@Test
	public void testCreateWithLocalReference() {
		Patient pt1 = new Patient();
		pt1.addName().addFamily("Everything").addGiven("Arthur");
		IIdType ptId1 = myPatientDao.create(pt1).getId().toUnqualifiedVersionless();

		Questionnaire q1 = new Questionnaire();
		q1.addItem().setLinkId("link1").setType(QuestionnaireItemType.STRING);
		IIdType qId = myQuestionnaireDao.create(q1).getId().toUnqualifiedVersionless();
		
		QuestionnaireResponse qr1 = new QuestionnaireResponse();
		qr1.getQuestionnaire().setReferenceElement(qId);
		qr1.setStatus(QuestionnaireResponseStatus.COMPLETED);
		qr1.addItem().setLinkId("link1").addAnswer().setValue(new DecimalType(123));
		try {
			ourClient.create().resource(qr1).execute();
			fail();
		} catch (UnprocessableEntityException e) {
			assertThat(e.toString(), containsString("Answer to question with linkId[link1] found of type [DecimalType] but this is invalid for question of type [string]"));
		}
	}
	
	@Test
	public void testCreateWithAbsoluteReference() {
		Patient pt1 = new Patient();
		pt1.addName().addFamily("Everything").addGiven("Arthur");
		IIdType ptId1 = myPatientDao.create(pt1).getId().toUnqualifiedVersionless();

		Questionnaire q1 = new Questionnaire();
		q1.addItem().setLinkId("link1").setType(QuestionnaireItemType.STRING);
		IIdType qId = myQuestionnaireDao.create(q1).getId().toUnqualifiedVersionless();
		
		QuestionnaireResponse qr1 = new QuestionnaireResponse();
		qr1.getQuestionnaire().setReferenceElement(qId.withServerBase("http://example.com", "Questionnaire"));
		qr1.setStatus(QuestionnaireResponseStatus.COMPLETED);
		qr1.addItem().setLinkId("link1").addAnswer().setValue(new DecimalType(123));
		try {
			ourClient.create().resource(qr1).execute();
			fail();
		} catch (UnprocessableEntityException e) {
			assertThat(e.toString(), containsString("Answer to question with linkId[link1] found of type [DecimalType] but this is invalid for question of type [string]"));
		}
	}

}
