package cz.cesnet.shongo.client.web.controllers;

import com.google.common.base.Strings;
import cz.cesnet.shongo.client.web.ClientWebConfiguration;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.ErrorModel;
import cz.cesnet.shongo.client.web.models.ReportModel;
import cz.cesnet.shongo.controller.ControllerConnectException;
import cz.cesnet.shongo.util.PasswordAuthenticator;
import net.tanesha.recaptcha.ReCaptcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.security.auth.login.Configuration;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Properties;

/**
 * Error controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"error", "report"})
public class ErrorController
{
    private static Logger logger = LoggerFactory.getLogger(ErrorController.class);

    @Resource
    private ClientWebConfiguration configuration;

    @Resource
    private ReCaptcha reCaptcha;

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT, method = {RequestMethod.GET})
    public ModelAndView handleReport(
            @RequestParam(value = "url", required = false) String requestUri)
    {
        ModelAndView modelAndView = new ModelAndView("report");
        modelAndView.addObject("report", new ReportModel(requestUri, reCaptcha));
        return modelAndView;
    }

    /**
     * Handle report problem.
     */
    @RequestMapping(value = ClientWebUrl.REPORT, method = {RequestMethod.POST})
    public ModelAndView handleReportSubmit(
            HttpServletRequest request,
            SessionStatus sessionStatus,
            @ModelAttribute("report") ReportModel reportModel,
            BindingResult bindingResult)
    {
        reportModel.validate(bindingResult, request);
        if (bindingResult.hasErrors()) {
            return new ModelAndView("report");
        }
        else {
            sendReport(reportModel, null, request);
            sessionStatus.setComplete();

            ModelAndView modelAndView = new ModelAndView("report");
            modelAndView.addObject("isSubmitted", true);
            return modelAndView;
        }
    }

    /**
     * Handle error view.
     */
    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response)
    {
        response.setHeader("Content-Type", "text/html; charset=UTF-8");

        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        ErrorModel errorModel = new ErrorModel(requestUri, statusCode, message, throwable, request);
        return handleError(errorModel, configuration, reCaptcha);
    }

    /**
     * Handle error report problem.
     */
    @RequestMapping("/error/submit")
    public ModelAndView handleErrorReportSubmit(
            HttpServletRequest request,
            SessionStatus sessionStatus,
            @ModelAttribute("error") ErrorModel errorModel,
            @ModelAttribute("report") ReportModel reportModel,
            BindingResult bindingResult)
    {
        reportModel.validate(bindingResult, request);
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("error");
            modelAndView.addObject("error", errorModel);
            modelAndView.addObject("report", reportModel);
            return modelAndView;
        }
        else {
            sendReport(reportModel, errorModel, request);
            sessionStatus.setComplete();

            ModelAndView modelAndView = new ModelAndView("report");
            modelAndView.addObject("isSubmitted", true);
            return modelAndView;
        }
    }

    /**
     * Handle error not found.
     */
    @RequestMapping("/error-not-found")
    public String handleErrorNotFound()
    {
        return "errorNotFound";
    }

    /**
     * Handle login error view.
     */
    @RequestMapping("/login-error")
    public ModelAndView handleLoginErrorView(HttpServletRequest request)
    {
        Exception exception = (Exception) request.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (exception != null) {
            Throwable exceptionCause = exception.getCause();
            if (exceptionCause instanceof ControllerConnectException) {
                return new ModelAndView(handleControllerNotAvailableView());
            }
        }
        ErrorModel errorModel = new ErrorModel(request.getRequestURI(), null, "Login error", exception, request);
        return handleError(errorModel, configuration, reCaptcha);
    }

    /**
     * Handle controller not available view.
     */
    @RequestMapping(value = "/controller-not-available")
    public String handleControllerNotAvailableView()
    {
        return "controllerNotAvailable";
    }

    /**
     * Raise test error.
     */
    @RequestMapping(value = "/test-error")
    public String handleTestError()
    {
        throw new RuntimeException("Test error");
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.HOME + ".", exception);
        return "redirect:" + ClientWebUrl.HOME;
    }

    /**
     * Send report.
     *
     * @param reportModel
     * @param errorModel
     * @param request
     */
    private void sendReport(ReportModel reportModel, ErrorModel errorModel, HttpServletRequest request)
    {
        String subject = "Problem report";
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("From: ");
        contentBuilder.append(reportModel.getEmail());
        contentBuilder.append("\n\n");
        contentBuilder.append(reportModel.getMessage());
        contentBuilder.append("\n\n");
        contentBuilder.append("--------------------------------------------------------------------------------\n\n");
        contentBuilder.append(reportModel.getContext().toString(request));
        if (errorModel != null) {
            subject = errorModel.getEmailSubject() + " - User report";
            contentBuilder.append("\n\n");
            contentBuilder.append(errorModel.getEmailContent());
        }
        sendEmailToAdministrator(subject, contentBuilder.toString(), configuration);
    }

    /**
     * Handle error.
     *
     *
     *
     * @param errorModel
     * @param configuration
     * @param reCaptcha
     * @return error {@link ModelAndView}
     */
    public static ModelAndView handleError(ErrorModel errorModel, ClientWebConfiguration configuration,
            ReCaptcha reCaptcha)
    {
        String emailSubject = errorModel.getEmailSubject();
        logger.error(emailSubject, errorModel.getThrowable());
        sendEmailToAdministrator(emailSubject, errorModel.getEmailContent(), configuration);

        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("error", errorModel);
        modelAndView.addObject("report", new ReportModel(errorModel.getRequestUri(), reCaptcha));
        return modelAndView;
    }

    private static void sendEmailToAdministrator(String subject, String content, ClientWebConfiguration configuration)
    {
        String administratorEmail = configuration.getAdministratorEmail();
        if (Strings.isNullOrEmpty(administratorEmail)) {
            logger.warn("Administrator email for sending error reports is not configured.");
            return;
        }
        if (Strings.isNullOrEmpty(configuration.getSmtpHost())) {
            logger.warn("SMTP host for sending error reports is not configured.");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", configuration.getSmtpHost());
        properties.setProperty("mail.smtp.port", configuration.getSmtpPort());
        if (!configuration.getSmtpPort().equals("25")) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }

        Authenticator authenticator = null;
        String smtpUserName = configuration.getSmtpUserName();
        if (!Strings.isNullOrEmpty(smtpUserName)) {
            properties.setProperty("mail.smtp.auth", "true");
            authenticator = new PasswordAuthenticator(smtpUserName, configuration.getSmtpPassword());
        }

        Session session = Session.getDefaultInstance(properties, authenticator);

        String sender = configuration.getSmtpSender();
        String subjectPrefix = configuration.getSmtpSubjectPrefix();
        try {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(content, "text/plain; charset=utf-8");

            StringBuilder html = new StringBuilder();
            html.append("<html><body><pre>");
            html.append(content);
            html.append("</pre></body></html>");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html.toString(), "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(sender));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(administratorEmail));
            mimeMessage.setSubject(subjectPrefix + subject);
            mimeMessage.setContent(multipart);

            logger.debug("Sending email from '{}' to '{}'...", new Object[]{sender, administratorEmail});
            Transport.send(mimeMessage);
        }
        catch (MessagingException exception) {
            logger.error("Failed to send error report.", exception);
        }
    }
}
