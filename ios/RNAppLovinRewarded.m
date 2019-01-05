
#import "RNAppLovinRewarded.h"

#if __has_include(<React/RCTUtils.h>)
#import <React/RCTUtils.h>
#else
#import "RCTUtils.h"
#endif

static NSString *const kEventAdLoaded = @"rewardedVideoAdLoaded";
static NSString *const kEventAdFailedToLoad = @"rewardedVideoAdFailedToLoad";
static NSString *const kEventAdOpened = @"rewardedVideoAdOpened";
static NSString *const kEventAdClosed = @"rewardedVideoAdClosed";
static NSString *const kEventRewarded = @"rewardedVideoAdRewarded";
static NSString *const kEventVideoStarted = @"rewardedVideoAdVideoStarted";
static NSString *const kEventVideoCompleted = @"rewardedVideoAdVideoCompleted";

@implementation RNAppLovinRewarded
{
    RCTPromiseResolveBlock _requestAdResolve;
    RCTPromiseRejectBlock _requestAdReject;
    BOOL hasListeners;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents
{
    return @[
             kEventRewarded,
             kEventAdLoaded,
             kEventAdFailedToLoad,
             kEventAdOpened,
             kEventVideoStarted,
             kEventAdClosed,
             kEventVideoCompleted ];
}

#pragma mark exported methods

RCT_EXPORT_METHOD(requestAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    _requestAdResolve = resolve;
    _requestAdReject = reject;

    if ([ALIncentivizedInterstitialAd isReadyForDisplay]) {
      reject(@"E_AD_ALREADY_LOADED", @"Ad is already loaded.", nil);
    } else {
      [ALIncentivizedInterstitialAd shared].adDisplayDelegate = self;
      [ALIncentivizedInterstitialAd preloadAndNotify: self];
    }
}

RCT_EXPORT_METHOD(showAd:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([ALIncentivizedInterstitialAd isReadyForDisplay]) {
      // Show call if using a reward delegate.
      [ALIncentivizedInterstitialAd showAndNotify:self];
      resolve(nil);
    }
    else {
      reject(@"E_AD_NOT_READY", @"Ad is not ready.", nil);
    }
}

RCT_EXPORT_METHOD(isReady:(RCTResponseSenderBlock)callback)
{
    callback(@[[NSNumber numberWithBool:[ALIncentivizedInterstitialAd isReadyForDisplay]]]);
}

- (void)startObserving
{
    hasListeners = YES;
}

- (void)stopObserving
{
    hasListeners = NO;
}

#pragma mark AdLoadDelegate

- (void)adService:(ALAdService *)adService didLoadAd:(ALAd *)ad {
  if (hasListeners) {
    [self sendEventWithName:kEventAdLoaded body:nil];
  }
  _requestAdResolve(nil);
}

- (void)adService:(ALAdService *)adService didFailToLoadAdWithError:(int)code {
  NSString *errorReason = [NSString stringWithFormat:@"Error code: %d", code];
  NSError *error = [[NSError alloc] initWithDomain:@"" code:code userInfo:@{ NSLocalizedFailureReasonErrorKey:errorReason}];
  if (hasListeners) {
    NSDictionary *jsError = RCTJSErrorFromCodeMessageAndNSError(@"E_AD_FAILED_TO_LOAD", error.localizedDescription, error);
    [self sendEventWithName:kEventAdFailedToLoad body:jsError];
  }
  
  _requestAdReject(@"E_AD_FAILED_TO_LOAD", error.localizedDescription, error);
}

#pragma mark - AdDisplayDelegate Methods

- (void)ad:(ALAd *)ad wasClickedIn:(UIView *)view {}
- (void)ad:(ALAd *)ad wasDisplayedIn:(UIView *)view {}

- (void)ad:(ALAd *)ad wasHiddenIn:(UIView *)view {
  if (hasListeners) {
    [self sendEventWithName:kEventAdClosed body:nil];
  }
}

#pragma mark VideoPlaybackDelegate

- (void)videoPlaybackBeganInAd:(ALAd *)ad {
  if (hasListeners) {
    [self sendEventWithName:kEventVideoStarted body:nil];
  }
}

- (void)videoPlaybackEndedInAd:(ALAd *)ad atPlaybackPercent:(NSNumber *)percentPlayed fullyWatched:(BOOL)wasFullyWatched {
  if (wasFullyWatched && hasListeners) {
    [self sendEventWithName:kEventVideoCompleted body:nil];
  }
}

#pragma mark RewardDelegate

- (void)rewardValidationRequestForAd:(ALAd *)ad didSucceedWithResponse:(NSDictionary *)response {
  [self sendEventWithName:kEventRewarded body:nil];
}

- (void)rewardValidationRequestForAd:(ALAd *)ad didExceedQuotaWithResponse:(NSDictionary *)response {
}

- (void)rewardValidationRequestForAd:(ALAd *)ad wasRejectedWithResponse:(NSDictionary *)response {

}

- (void)rewardValidationRequestForAd:(ALAd *)ad didFailWithError:(NSInteger)responseCode {

}

@end
  
