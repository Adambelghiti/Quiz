/**
 * Minimal ambient types for the `qrcode` package.
 *
 * The published `@types/qrcode` package pulls in `@types/node`, whose newer
 * `Symbol.dispose` / `TextEncoder` declarations conflict with the TypeScript
 * version Angular 16 ships with. `angularx-qrcode` only re-exports the
 * error-correction-level type from `qrcode`, so that is all we declare.
 */
declare module 'qrcode' {
  export type QRCodeErrorCorrectionLevel =
    'low' | 'medium' | 'quartile' | 'high' | 'L' | 'M' | 'Q' | 'H';
}
