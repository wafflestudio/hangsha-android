# Gson 2.10.1에는 consumer rules가 없으므로 제네릭 타입과 JSON 어노테이션을 보존한다.
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Gson이 리플렉션으로 생성하는 통신 모델은 유지하되 클래스 이름은 난독화할 수 있다.
-keep,allowobfuscation class com.example.hangsha_android.data.network.model.**
# @SerializedName이 없는 필드의 JSON 키와 기본 생성자에 설정된 기본값을 보존한다.
-keepclassmembers class com.example.hangsha_android.data.network.model.** {
    <fields>;
    <init>();
}

# 기존 DataStore JSON을 업데이트 후에도 읽을 수 있도록 저장 모델의 필드 이름을 보존한다.
-keep,allowobfuscation class com.example.hangsha_android.data.local.StoredExcludedKeywordItem
-keepclassmembers class com.example.hangsha_android.data.local.StoredExcludedKeywordItem {
    <fields>;
}

# 통신 모델 밖에 있는 관심사 타입도 Gson의 enum 변환 대상이다.
-keep enum com.example.hangsha_android.data.repository.model.CategoryType {
    <fields>;
}

# Kakao SDK 공식 지침: SDK의 모델 필드를 보존한다.
# https://developers.kakao.com/docs/latest/ko/android/getting-started#proguard
-keep class com.kakao.sdk.**.model.* { <fields>; }

# Naver 5.11.2는 로그인 초기화 때 사용하지 않는 프로필 API도 Retrofit Proxy로 생성한다.
# 호출되지 않는 메서드가 제거되면 SDK의 조건부 keep 규칙이 풀려 초기화 중 ClassCastException이 발생한다.
-keep,allowobfuscation interface com.navercorp.nid.profile.data.service.NidProfileService {
    @retrofit2.http.* <methods>;
}

# 나머지 Retrofit/OkHttp 및 Naver SDK 규칙은 라이브러리의 consumer rules를 사용한다.
