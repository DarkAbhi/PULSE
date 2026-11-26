export const DEFAULT_CURRENCY = "GBP";
export const CURRENCIES = Object.freeze(["AED", "AFN", "ALL", "AMD", "ANG", "AOA", "ARS", "AUD", "AWG", "AZN", "BAM", "BBD", "BDT", "BGN", "BHD", "BIF", "BMD", "BND", "BOB", "BRL", "BSD", "BTN", "BWP", "BYN", "BZD", "CAD", "CDF", "CHF", "CLP", "CNY", "COP", "CRC", "CUC", "CUP", "CVE", "CZK", "DJF", "DKK", "DOP", "DZD", "EGP", "ERN", "ETB", "EUR", "FJD", "FKP", "GBP", "GEL", "GHS", "GIP", "GMD", "GNF", "GTQ", "GYD", "HKD", "HNL", "HRK", "HTG", "HUF", "IDR", "ILS", "INR", "IQD", "IRR", "ISK", "JMD", "JOD", "JPY", "KES", "KGS", "KHR", "KMF", "KPW", "KRW", "KWD", "KYD", "KZT", "LAK", "LBP", "LKR", "LRD", "LSL", "LYD", "MAD", "MDL", "MGA", "MKD", "MMK", "MNT", "MOP", "MRU", "MUR", "MVR", "MWK", "MXN", "MYR", "MZN", "NAD", "NGN", "NIO", "NOK", "NPR", "NZD", "OMR", "PAB", "PEN", "PGK", "PHP", "PKR", "PLN", "PYG", "QAR", "RON", "RSD", "RUB", "RWF", "SAR", "SBD", "SCR", "SDG", "SEK", "SGD", "SHP", "SLE", "SLL", "SOS", "SRD", "SSP", "STN", "SVC", "SYP", "SZL", "THB", "TJS", "TMT", "TND", "TOP", "TRY", "TTD", "TWD", "TZS", "UAH", "UGX", "USD", "UYU", "UZS", "VES", "VND", "VUV", "WST", "XAF", "XCD", "XCG", "XOF", "XPF", "YER", "ZAR", "ZMW", "ZWG", "ZWL"]);
const currencyCodes = new Set(CURRENCIES);
const currencyNames = new Intl.DisplayNames(["en"], { type: "currency" });
/** @type {Map<string, Intl.NumberFormat>} */
const formatters = new Map();

export function validCurrency(code) { return currencyCodes.has(code); }
export function currencyLabel(code) { return `${code} — ${currencyNames.of(code)}`; }
function moneyFormatter(currency, locale) {
 if (!validCurrency(currency)) throw new Error("Unsupported currency");
 const key = `${locale}:${currency}`;
 const existing = formatters.get(key);
 if (existing) return existing;

 const formatter = new Intl.NumberFormat(locale, { style: "currency", currency, currencyDisplay: "narrowSymbol" });
 if (formatters.size >= 64) formatters.delete(formatters.keys().next().value);
 formatters.set(key, formatter);
 return formatter;
}
export function formatMoney(amount, currency = DEFAULT_CURRENCY, locale = "en-GB") {
 return moneyFormatter(currency, locale).format(amount);
}
export function formatMoneyParts(amount, currency = DEFAULT_CURRENCY, locale = "en-GB") {
 return moneyFormatter(currency, locale).formatToParts(amount);
}

export function currencySymbol(currency = DEFAULT_CURRENCY, locale = "en-GB") {
 return moneyFormatter(currency, locale)
   .formatToParts(0).find(part => part.type === "currency")?.value ?? currency;
}
