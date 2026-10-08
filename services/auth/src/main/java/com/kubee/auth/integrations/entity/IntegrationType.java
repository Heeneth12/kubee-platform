package com.kubee.auth.integrations.entity;

public enum IntegrationType {
    // Payment Gateways
    RAZORPAY,
    RAZORPAY_TEST,
    STRIPE,
    STRIPE_TEST,

    // Communication
    WHATSAPP,
    SLACK,

    // CRM / Productivity
    ZOHO,

    // Email
    SENDGRID,
    EMAIL_SMTP,
    EMAIL_SMTP_TEST,
    GMAIL,
    GMAIL_TEST,

    // Generic
    WEBHOOK_GENERIC
}
