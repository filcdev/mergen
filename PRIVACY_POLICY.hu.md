<!--
SABLON — közzététel előtt töltsd ki az összes <HELYŐRZŐT>, és nézesd át az iskola
adatvédelmi tisztviselőjével. Utána tedd közzé egy nyilvános URL-en, és azt az URL-t
add meg a Google Play Console-ban (App content → Privacy policy) és az App Store Connectben.
-->

# Adatvédelmi szabályzat — Mergen (Filcapp)

**Hatálybalépés dátuma:** `<HATÁLYBALÉPÉS DÁTUMA>`

**Adatkezelő:** `<ADATKEZELŐ NEVE>`, `<POSTAI CÍM>`

**Kapcsolat:** `<KAPCSOLATI E-MAIL>`

---

## 1. A szabályzatról

Ez a szabályzat elmagyarázza, hogy a Mergen („az Alkalmazás", csomagnév: `hu.petrik.filcapp`) hogyan kezeli a személyes adatokat. Az Alkalmazás a Filc iskolai rendszer mobil kliense, amelyet a `<ADATKEZELŐ NEVE>` („mi") üzemeltet. Az Alkalmazás az iskola diákjai és tanárai számára készült.

## 2. Röviden

- Csak azt az adatot kezeljük, amely a bejelentkezéshez és az órarend megjelenítéséhez szükséges.
- A bejelentkezés az **iskolai Microsoft-fiókkal** történik (Microsoft Entra ID).
- A hitelesítő adatokat **kizárólag az eszközén, titkosítva** tároljuk.
- Az Alkalmazás **nem** használ reklám-, analitikai vagy összeomlásjelentő szolgáltatást.
- Az órarendet és az iskolai híreket az iskola szervereiről töltjük le, hitelesített kapcsolaton.
- Adathozzáférési vagy törlési kérését a `<KAPCSOLATI E-MAIL>` címen jelezheti.

## 3. Milyen adatokat kezelünk

### 3.1 Fiók és személyazonosság (Microsoft Entra ID bejelentkezés)

Amikor iskolai Microsoft-fiókkal jelentkezik be, a következő adatokat kapjuk (`openid profile email offline_access`):

- az Ön neve / megjelenített neve,
- e-mail-címe,
- beceneve,
- fiókazonosítója, szerepkörei és jogosultságai,
- osztály- (cohort-) azonosítója.

**Cél:** az Ön hitelesítése és az Alkalmazás személyre szabása.

### 3.2 Az eszközön tárolt hitelesítő adatok

A bejelentkezés fenntartásához az Alkalmazás a következőket **titkosítva, az eszközén** tárolja:

- a Microsoft Entra ID tokent és frissítési (refresh) tokent,
- az iskolai backend munkamenet-sütijét (`filc.session_token`),
- a bejelentkezés ideiglenes adatait (PKCE-verifier, state, nonce).

Androidon ezeket az Android Keystore eszközhöz kötött kulcsával titkosítjuk; iOS-en a Keychainben tároljuk. Eszközbiztonsági mentésből nem olvashatók ki.

### 3.3 Profilbeállítások

Módosíthatja a becenevét, a kiválasztott osztályt/csoportot és az értesítési nyelvet. Ezeket az iskola backendje tárolja.

### 3.4 Az Alkalmazásban megjelenő tartalom

Órarend, helyettesítések, áthelyezett órák, tantermek, tanárnevek, közlemények és iskolai hírek. Ezek az Ön iskolai fiókjához kapcsolódnak; hitelesített kapcsolaton keresztül töltjük le és jelenítjük meg (az eszközre nem gyorsítótárazzuk).

### 3.5 Technikai adatok

Mint minden online szolgáltatásnál, az iskola szerverei technikai adatokat kezelnek (például IP-cím, a kérés időpontja) a tartalom kiszolgálásához és a szolgáltatás biztonságához.

**Nem** gyűjtünk helyadatot, névjegyeket, kamerát, fényképeket, fájlokat vagy hirdetési azonosítókat. Az Android-alkalmazás csak az **Internet** engedélyt kéri; az iOS-alkalmazás semmilyen különleges engedélyt nem kér.

## 4. Célok és jogalapok (GDPR 6. cikk)

- **Az Alkalmazás és az órarend biztosítása** — az iskola feladatának ellátása, illetve az iskolai szolgáltatás működtetéséhez fűződő jogos érdek.
- **Az Ön hitelesítése és a munkamenet biztonsága** — a szolgáltatás nyújtásához szükséges.
- **Iskolai hírek megjelenítése** — jogos érdek.

Adatait nem használjuk reklámozásra vagy profilalkotásra.

## 5. Kikkel osztjuk meg az adatokat

- **Microsoft** (Microsoft Entra ID, `login.microsoftonline.com`) — a bejelentkezéshez használt identitásszolgáltató.
- **Az iskola backendje („Chronos", `filc.petrik.hu`)** — tárolja a fiókját és profilbeállításait, valamint kiszolgálja az Alkalmazásban megjelenő tartalmat.
- **`petrik.hu`** (iskolai webhely) — névtelen kérés nyilvános hírekért; fiókadatot nem küldünk.

Nincs analitikai, reklám- vagy adatbróker-szolgáltatás. Személyes adatait nem adjuk el.

## 6. Nemzetközi adattovábbítás

A bejelentkezést a Microsoft kezeli, amely az adatokat az Európai Gazdasági Térségen kívül is feldolgozhatja megfelelő garanciák mellett (például az EU általános szerződési feltételei, SCC).

## 7. Megőrzés és törlés

- A hitelesítő adatok a kijelentkezésig maradnak az eszközén; a kijelentkezés törli őket.
- Az Alkalmazás eltávolítása minden helyben tárolt adatot töröl.
- Az iskola backendjén tárolt adatokat (fiók, profilbeállítások) az iskola a saját szabályai szerint őrzi meg. Törlési kéréshez forduljon a `<KAPCSOLATI E-MAIL>` címhez.

## 8. Biztonság

A helyi hitelesítő adatokat eszközhöz kötött kulcsokkal titkosítjuk. Minden kommunikáció HTTPS/TLS-en keresztül történik. A backend adataihoz csak hitelesített iskolai fiókkal lehet hozzáférni.

## 9. Gyermekek adatainak védelme

Az Alkalmazás iskolai eszköz, ezért 18 év alatti diákok is használhatják. Az adatokat az iskola oktatási szolgáltatásának részeként, az iskola hatáskörében kezeljük; nem jelenítünk meg reklámot, és nem végzünk viselkedésalapú profilalkotást. Ha úgy gondolja, hogy egy gyermek adatait nem megfelelően kezeljük, forduljon a `<KAPCSOLATI E-MAIL>` címhez.

## 10. Az Ön jogai

A GDPR alapján kérheti adataihoz való hozzáférést, azok helyesbítését, törlését, kezelésük korlátozását, az adathordozhatóságot, valamint tiltakozhat a kezelés ellen. Panaszt nyújthat be a felügyeleti hatóságnál (`<FELÜGYELETI HATÓSÁG>`; Magyarországon a Nemzeti Adatvédelmi és Információszabadság Hatóság, NAIH, https://naih.hu). Jogai gyakorlásához forduljon a `<KAPCSOLATI E-MAIL>` címhez.

## 11. A szabályzat változásai

A szabályzatot frissíthetjük. A hatálybalépés dátuma megváltozik, és jelentős változás esetén az Alkalmazásban vagy e-mailben értesítjük.

## 12. Kapcsolat

`<ADATKEZELŐ NEVE>`, `<POSTAI CÍM>` — `<KAPCSOLATI E-MAIL>`
