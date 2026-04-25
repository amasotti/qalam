# Changelog

All notable changes to this project will be documented in this file (created with git cliff).

## [0.3.0] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/4d8000034c8c0c8eb759db41f29739d57e2f333f..b71756355d810ae5dcbc536732ca0180aa5fdfc5)
### 🚀 Features


- Enhance VocabLookupDrawer with search input and improved lookup handling ([071e46d](https://github.com/amasotti/qalam/commit/071e46d3c9f26b7312925d97220538eedb6dffc3))

- Add sorting functionality to text filters with sortBy and sortDesc options ([9d6785f](https://github.com/amasotti/qalam/commit/9d6785f82e30e2a2d6d1e34d5d7c1d9bcb901c92))

- Extend service.list method to support additional parameters ([bd2d5cd](https://github.com/amasotti/qalam/commit/bd2d5cd79c750ffb87a87dfbed5b903b032bf2bf))

- Add sync endpoint to rebuild text from its sentences ([262f3f0](https://github.com/amasotti/qalam/commit/262f3f07a7a11d389dde6a479a5451878bdf6cf8))

- Update TextService to include SentenceRepository and improve test assertions ([109cdc5](https://github.com/amasotti/qalam/commit/109cdc5c3e36668d88b18aabebcba411dcbfacff))

- Enhance tokenization process to include transliteration and translation fields ([b717563](https://github.com/amasotti/qalam/commit/b71756355d810ae5dcbc536732ca0180aa5fdfc5))

### 🧪 Testing


- Fix e2e tests after refactoring in FE ([3ec126b](https://github.com/amasotti/qalam/commit/3ec126ba8bcc020eccac12bde1c0e063822a3511))

- Fix e2e tests after refactoring in FE ([51884ad](https://github.com/amasotti/qalam/commit/51884ad967402deb8d2887dc7c136bb7d1bf0707))

### ⚙️ Miscellaneous Tasks


- Run formatter ([2e57293](https://github.com/amasotti/qalam/commit/2e57293e87eb50da9e9fb32e4e8f84abaf90f586))

## [0.2.0] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/bc0a2050836cc4a6152636ad40e0c8acd154c402..4d8000034c8c0c8eb759db41f29739d57e2f333f)
### 🚀 Features


- Add GET /words/by-arabic exact-text lookup endpoint ([a02738f](https://github.com/amasotti/qalam/commit/a02738fd38871330de359abcf6c245be7bb2ff9e))

- Add POST /words/analyze AI word analysis endpoint ([be3a94d](https://github.com/amasotti/qalam/commit/be3a94dd27a80f5cb6ed3f6ffc547aa43cbd9fe0))

- Add useLookupWordByArabic and useAnalyzeWord store hooks ([336cc91](https://github.com/amasotti/qalam/commit/336cc9107b10540c068c299f39c078dba8ec74c5))

- Add VocabLookupDrawer component and integrate with InterlinearSentence ([1b6c52c](https://github.com/amasotti/qalam/commit/1b6c52cc476924ceca00864f3bed6b7a798e9ebf))

- Add word lookup and analysis functionality with updated API integration ([ca7c44f](https://github.com/amasotti/qalam/commit/ca7c44fff7e3565c8f03687ce6294df48ca6cc98))

- Update annotation types in AnnotationForm component ([4d80000](https://github.com/amasotti/qalam/commit/4d8000034c8c0c8eb759db41f29739d57e2f333f))

### 🐛 Bug Fixes


- Update locator class for root hero Arabic visibility check ([a6c0b9d](https://github.com/amasotti/qalam/commit/a6c0b9d92d8c9befd6d0243fb6bc0f184861a2c8))

### 🚜 Refactor


- Widen token click callback to pass full AlignmentTokenResponse ([0362523](https://github.com/amasotti/qalam/commit/036252314080e392a7b2ff7149a1f381aa77e10b))

- Update banner.svg for improved color scheme and design consistency ([f5bb497](https://github.com/amasotti/qalam/commit/f5bb497a041d2f6fa8a2281aa2722da5d9eacbcb))

### ⚙️ Miscellaneous Tasks


- Remove unused layout class ([29992df](https://github.com/amasotti/qalam/commit/29992df036e758378f5f330adb04220485331938))

## [0.1.0] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/846ad63f5153d12aa3f22c02ab7cf4488b986918..bc0a2050836cc4a6152636ad40e0c8acd154c402)
### 🚜 Refactor


- Redesign sidebar layout and improve styling ([df0b3aa](https://github.com/amasotti/qalam/commit/df0b3aa058820846d8fe7c610be44c281154b183))

- Update layout.css for Busatan design system and remove legacy classes ([f87629e](https://github.com/amasotti/qalam/commit/f87629e8eaf2d57a19a56b3b09d9c17ad1386a1f))

- Enhance layout and styling for word and text detail pages ([cf3551c](https://github.com/amasotti/qalam/commit/cf3551c0aeeb4d1426cfd172682fd29f11252fdc))

- Improve layout and styling for word examples and dictionary links ([95b16ff](https://github.com/amasotti/qalam/commit/95b16ff45d45b0d411482ac41cc10d5e192f072a))

- Update font families and improve CSS variables for better design consistency ([3529a14](https://github.com/amasotti/qalam/commit/3529a1496ff8871d06e435fae83c491f698f4859))

- Replace font family from Lora to Spectral for improved design consistency ([2584c3b](https://github.com/amasotti/qalam/commit/2584c3b9c2d53a8e8b5a37703821856fdb2f397d))

### ⚙️ Miscellaneous Tasks


- Lint layout.css ([48c72ad](https://github.com/amasotti/qalam/commit/48c72ade5a2f2fdd67182ac35683e6a0de241601))

- Remove implemented plan ([bc0a205](https://github.com/amasotti/qalam/commit/bc0a2050836cc4a6152636ad40e0c8acd154c402))

## [0.0.4] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/3a9342647ce31898ba01ae56cd0534a85062ead7..846ad63f5153d12aa3f22c02ab7cf4488b986918)
### 🚀 Features


- Annotation feature ([a863ef5](https://github.com/amasotti/qalam/commit/a863ef5ab0754a9f9911ddeb15a2d733e8178cc1))

- Integrate annotation drawer functionality ([ac26b97](https://github.com/amasotti/qalam/commit/ac26b97d9f199e9fba2d8b7d04b175030d5a88f2))

- 2 additional commands for claude ([48f6e1d](https://github.com/amasotti/qalam/commit/48f6e1dc936ae7281b4da87fa81624bea33bfc6d))

### 🐛 Bug Fixes


- Release workflow ([db864b6](https://github.com/amasotti/qalam/commit/db864b68508e603aa11475dcf085d600ce790e0e))

### 🚜 Refactor


- Remove inline opt-in annotation (add at compiler level) ([0734e4b](https://github.com/amasotti/qalam/commit/0734e4b837d01242bab90610952b730904c5b7bc))

### ⚙️ Miscellaneous Tasks


- Update design mockups ([96e6696](https://github.com/amasotti/qalam/commit/96e669601204347663b5a75f1101233e7281ff82))

- Cleanup after implementation ([0a6dca2](https://github.com/amasotti/qalam/commit/0a6dca20ffe564a3512a9ae838da165a93cf2650))

- Improve tokenize command ([117f550](https://github.com/amasotti/qalam/commit/117f550198c7ba058a934d0bbd93486eadd302ba))

- Format fe files ([e126fdb](https://github.com/amasotti/qalam/commit/e126fdb9546ea478249e44227bee77e0eba688ff))

## [0.0.3] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/9944d59c1031623ad02979188efbcb7d694d8c83..3a9342647ce31898ba01ae56cd0534a85062ead7)
### 🚀 Features


- Add training_sessions and training_session_words tables ([304a392](https://github.com/amasotti/qalam/commit/304a3925ae7ee45e19fe615324b8315ccd9a3132))

- Add training domain types and error variants ([0fb4b39](https://github.com/amasotti/qalam/commit/0fb4b396dd9f8d65c162b6daa28e69e0acdcff8e))

- Add training DTOs ([3797d11](https://github.com/amasotti/qalam/commit/3797d1174dd0703640125a913a1a67c9f54c2f61))

- Add TrainingRepository interface and extend WordRepository ([f02da08](https://github.com/amasotti/qalam/commit/f02da08d59fe15b339fe480a6e97d872739be0b1))

- Add Exposed table objects for training ([d3a1f9d](https://github.com/amasotti/qalam/commit/d3a1f9dd5c85f86827251e883daeae35ecb1e92d))

- Implement training queries in ExposedWordRepository ([1cbe7a7](https://github.com/amasotti/qalam/commit/1cbe7a7955dc0abfe3fb04e3071903fd6d1a46ca))

- Update README with project description, features, and setup instructions ([7c55983](https://github.com/amasotti/qalam/commit/7c5598337ffc8dfde27c03ab6e03c81b1526e22e))

- Enhance layout for root info and word family sections ([c761eac](https://github.com/amasotti/qalam/commit/c761eacdf3120c38b1896619feb4cf1c13a8753b))

- Implement ExposedTrainingRepository ([cfb8d9e](https://github.com/amasotti/qalam/commit/cfb8d9e61fecac7aeb7a9874c3bb8774ff0bed75))

- Implement TrainingService with mastery promotion logic ([84b2354](https://github.com/amasotti/qalam/commit/84b235437ef93d28d90187269dfe2dda3ad8141f))

- Add TrainingRoutes, wire DI and routing ([7c20756](https://github.com/amasotti/qalam/commit/7c207568f7cfe19eaee4819fb0e3d2a9919f295e))

- Add training API endpoints and data models for session management and statistics ([c5ab291](https://github.com/amasotti/qalam/commit/c5ab291e5f98771b67cd6dede9578dceb2cb8576))

- Add training store with session and result mutations ([9b0987f](https://github.com/amasotti/qalam/commit/9b0987fd1f2956887bd28c346adb95fdb82f1dd2))

- Add SessionSummary component ([338666b](https://github.com/amasotti/qalam/commit/338666baf12e2687a3c6c8df07f93cf8f5050758))

- Add FlashCard component for training flashcard view ([2343b94](https://github.com/amasotti/qalam/commit/2343b94233dee020e7af7b8298b2fec57b081307))

- Add training setup page ([cff4128](https://github.com/amasotti/qalam/commit/cff41288e5c8e7304dda63889e7a6586ba7ac89d))

- Add training session page with flashcard flow ([83bf25e](https://github.com/amasotti/qalam/commit/83bf25eb8623f8dd2337891dd037eade4047c009))

- Enhance FlashCard component to handle result callbacks ([0adcddc](https://github.com/amasotti/qalam/commit/0adcddcf529e59329cbe1cc47a8d5c02093bc03b))

- Add claude commands ([3a93426](https://github.com/amasotti/qalam/commit/3a9342647ce31898ba01ae56cd0534a85062ead7))

### 🐛 Bug Fixes


- Release workflow dispatching ([3cace04](https://github.com/amasotti/qalam/commit/3cace042079d6e2bd69260b1d3ac4d564316cfe5))

- FindForTraining mastery filter and updateProgress guard ([5f9f169](https://github.com/amasotti/qalam/commit/5f9f1690405642b0ffb0384cb4d8133382837a2a))

- Align Instant type to kotlin.time across training domain ([0086931](https://github.com/amasotti/qalam/commit/00869315b718179ef1c79740c5921557dd63ecc5))

- Suppress detekt TooGenericExceptionCaught and SwallowedException in training repositories ([02ed890](https://github.com/amasotti/qalam/commit/02ed8907ac3a2bb4dffec512a0343dc78fcd6fde))

- Ci pipeline ([8bf47b4](https://github.com/amasotti/qalam/commit/8bf47b4a0be85dfaa645b95c4fdb5f5df3fb5137))

- Use mutate + Button component in training setup page ([b0ae01e](https://github.com/amasotti/qalam/commit/b0ae01e4c689db6dd147ad28cbd151a20aed35c6))

- Update FlashCard component to use local state for unanswered words ([13623a6](https://github.com/amasotti/qalam/commit/13623a6d9352df2acf90a45c8b49fb0b860d6e26))

- Handle missing URL templates in DictionaryLinks component ([11b0b92](https://github.com/amasotti/qalam/commit/11b0b926a4f101a6217860a03b02b1ecba81253c))

- Add .superpowers to .gitignore ([bd6ea5d](https://github.com/amasotti/qalam/commit/bd6ea5d5bd7856aed7ab0edca844019c16479321))

### 🚜 Refactor


- Remove summarizeText API and related types from SDK ([996f303](https://github.com/amasotti/qalam/commit/996f3039dfd1b53d7928a199e8c6c0ab7083493a))

### 🧪 Testing


- Improve error handling in word family test ([c100f16](https://github.com/amasotti/qalam/commit/c100f162f297ccc6988675b70035b188da8194ef))

- Mastery promotion unit tests ([317ff49](https://github.com/amasotti/qalam/commit/317ff49c24d8fe78088de30e604396f3a1905fe4))

- Training API integration tests ([c2daf27](https://github.com/amasotti/qalam/commit/c2daf274fb9a38cd8d38fab8dff716e0af77afeb))

### ⚙️ Miscellaneous Tasks


- Remove migration scripts (all done) ([5864f09](https://github.com/amasotti/qalam/commit/5864f096afa70dd513aae34f0d90a6f220435c5e))

- Remove migration scripts (all done) ([11b1034](https://github.com/amasotti/qalam/commit/11b103491be3c14e4afd4415582788eeee2e05a6))

- Cleanup openapi and regenerate types ([164d162](https://github.com/amasotti/qalam/commit/164d16244d5f9681ae33fa52705d231ccd0489c3))

- Cformat flashcard page ([f0a1fd2](https://github.com/amasotti/qalam/commit/f0a1fd2dc801d45253743b8931f12c0adb5cc4b4))

- Cleanup fe page ([2297c81](https://github.com/amasotti/qalam/commit/2297c8120d7c206fbf077a6dda42c7b54696fe65))

- Cleanup fe page ([6d53eba](https://github.com/amasotti/qalam/commit/6d53ebad9e123120fc5ac25dfe2dfe62ee1530a2))

- Remove unnecessary whitespace in +page.svelte ([51b88b6](https://github.com/amasotti/qalam/commit/51b88b638589df958a2661c7dfda61650061d6f0))

- Add mockups - FE refresh ([5172370](https://github.com/amasotti/qalam/commit/5172370de210784701120a6966c4ffbf12337375))

- Update claude rules ([d4e0f75](https://github.com/amasotti/qalam/commit/d4e0f759c79344891efbd75fe897b83f1c2ac436))

- Update specs ([529d99d](https://github.com/amasotti/qalam/commit/529d99df98c239cea8d7a260f6f8a159cfa8d72d))

- Update specs ([51dbd41](https://github.com/amasotti/qalam/commit/51dbd410fceb216e5e9b3983eb94e64466cc174e))

- Add future plans ([80e8679](https://github.com/amasotti/qalam/commit/80e8679780fc96c450d254fa4e3e0a557c3b08f9))

### E2e


- Add new tests ([217330d](https://github.com/amasotti/qalam/commit/217330d916d8e33a043eee04d5b8c16c57d9c256))

## [0.0.2] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/4df60e3968dd80b8be2cf9df44c0515ea4306326..9944d59c1031623ad02979188efbcb7d694d8c83)
### 🚀 Features


- Add Conflict error type for uniqueness constraint violations ([6b654d5](https://github.com/amasotti/qalam/commit/6b654d581b32fe86dfea4597c25eb8692eebcf86))

- Add OpenAPI smoke test for route registration in Ktor ([63862ad](https://github.com/amasotti/qalam/commit/63862adb28ea484620777cd5a6326faa1c27d8a3))

- Add error page component for improved user feedback ([9d9cba3](https://github.com/amasotti/qalam/commit/9d9cba3cde572d97260a25d84a965e832b5c8d1a))

- Add Playwright E2E tests for navigation, roots, texts, and words ([b75125a](https://github.com/amasotti/qalam/commit/b75125a223050392e6eb710c3841ebd37b24eacb))

### 🐛 Bug Fixes


- Release workflow dispatching ([9944d59](https://github.com/amasotti/qalam/commit/9944d59c1031623ad02979188efbcb7d694d8c83))

### 🚜 Refactor


- Replace parameter extraction with getOrFail for improved error handling ([0e619eb](https://github.com/amasotti/qalam/commit/0e619eb0cab14d6641d558b9e24238bcb06324b2))

- Remove default AiClient instantiation in WordService constructor ([9689718](https://github.com/amasotti/qalam/commit/9689718c4e2eaf7b17eed2bcb978a2a094309a1f))

### 🧪 Testing


- Improve error handling in word family test ([93b19aa](https://github.com/amasotti/qalam/commit/93b19aa346ee2d28dfcfb92b33a79696bce36598))

### ⚙️ Miscellaneous Tasks


- Add CI checks before release job ([8fc7ea1](https://github.com/amasotti/qalam/commit/8fc7ea104e36e8df948fbabfeed35f7e9ef71d3f))

- Remove useless constructor argument for AI client ([d8de126](https://github.com/amasotti/qalam/commit/d8de12674c8587d62b863b55b85f76a3594dc32b))

- Drop duplicate TRGM indexes for Arabic words and translations ([c765718](https://github.com/amasotti/qalam/commit/c765718ee077472cb7fc05fc351a187797fe64f7))

- Fix tests and run formatter ([d9b595c](https://github.com/amasotti/qalam/commit/d9b595c23ce829178e995a377730782e1a221ba4))

### E2e


- Add new tests ([61465a7](https://github.com/amasotti/qalam/commit/61465a7eadcbb05426142f7a2cef323aaaab8230))

## [0.0.1] - 2026-04-25

[Compare with last version](https://github.com/amasotti/qalam/compare/19fe633fd2d01cf1df88f0f99c03f676fffafa4d..4df60e3968dd80b8be2cf9df44c0515ea4306326)
### 🚀 Features


- Add auto-tokenizer skill for claude ([2428728](https://github.com/amasotti/qalam/commit/2428728469df51d2540e1a4550aa5d1dbfca435c))

### ⚙️ Miscellaneous Tasks


- Improve prompt ([36ffd17](https://github.com/amasotti/qalam/commit/36ffd177cff03d1b734cc7dbf93b42953ed8c37c))

- Claude permissions ([49c6d1b](https://github.com/amasotti/qalam/commit/49c6d1b50033ce7225fe735066f9397587c21f6b))

## [0.0.1-alpha.2] - 2026-04-19

[Compare with last version](https://github.com/amasotti/qalam/compare/0646e7cbf525a08e8fc2672b044b5095d6bf89a4..19fe633fd2d01cf1df88f0f99c03f676fffafa4d)
### 🚀 Features


- Add CI and release workflows for automated testing and versioning ([ce18a3c](https://github.com/amasotti/qalam/commit/ce18a3cac46971c478ad00311adfd42bf9ad4331))

- Implement reorder sentences feature with API integration and UI updates ([b426bfd](https://github.com/amasotti/qalam/commit/b426bfd0ece60f46c5024329d6ba0c296d21bd59))

- Add Docker configuration with Traefik and Caddy for frontend and backend services ([756493a](https://github.com/amasotti/qalam/commit/756493a2a5b8c17edb78530537c87c086c9f0260))

- M12 - backups ([683addc](https://github.com/amasotti/qalam/commit/683addc4fe9ca65434db8c360a93cf46d24ea77a))

- Migrate from an-na7wi ([4ac5faa](https://github.com/amasotti/qalam/commit/4ac5faaeaea0e649d5e6a652dbfd07b4235665c5))

- Backup sync script ([6d6b1bc](https://github.com/amasotti/qalam/commit/6d6b1bc4afe9d2894903863308e6113855252b09))

- Add rootId filter to word queries and enhance word chip UI ([4c86f85](https://github.com/amasotti/qalam/commit/4c86f85649a95fd46ce658d8312cb6c463b90f22))

### 🐛 Bug Fixes


- Fix cors problem ([a32a08a](https://github.com/amasotti/qalam/commit/a32a08a3e5e17e4f800b554c53b79625a4bfdbb0))

### ⚙️ Miscellaneous Tasks


- Add Redocly configuration file with custom rules ([90ac58b](https://github.com/amasotti/qalam/commit/90ac58b6cf0d0c07e7e028e0683c12992851c281))

- Fix api documenation ([9f84037](https://github.com/amasotti/qalam/commit/9f84037a3cf8d165b201c6d4db2e201054d8b2cc))

- Update justfile ([16b0738](https://github.com/amasotti/qalam/commit/16b0738bf86b40d70fedf57dc20cab98f0e8c1d6))

- Prepare migration from an-na7wi ([1524f66](https://github.com/amasotti/qalam/commit/1524f66be14ff05f53edb59b4cd42c34e962fb3c))

- Prepare migration ([76c8c3b](https://github.com/amasotti/qalam/commit/76c8c3b9a99e5df0026c06e383c2c1ba590a53d5))

- Re-run type generation ([19fe633](https://github.com/amasotti/qalam/commit/19fe633fd2d01cf1df88f0f99c03f676fffafa4d))

## [0.0.1-alpha.1] - 2026-04-19

### 🚀 Features


- Blank init repo ([ea870c4](https://github.com/amasotti/qalam/commit/ea870c48b2bc0f5b6ead841650f92533cacbcfd1))

- Spec driven steering files ([8304158](https://github.com/amasotti/qalam/commit/83041583b10c55f2ef99395d48565b4b633c8b21))

- Setup backend ([4691fcd](https://github.com/amasotti/qalam/commit/4691fcd2b6e64767f0e52df002874621b825448e))

- Nix setup ([c6c9fac](https://github.com/amasotti/qalam/commit/c6c9fac608c54cf5991934dce719f86ea50e8f74))

- Add initial application structure and dependencies ([82f2de9](https://github.com/amasotti/qalam/commit/82f2de9be71f9dec839085c34040fa77c59507d4))

- *(M0)* Add CI configuration for backend tests with Gradle ([8c21eff](https://github.com/amasotti/qalam/commit/8c21eff7fb564edc2795e0d1dc564baba0454338))

- Update CI configuration to use Nix and improve test execution ([273d91e](https://github.com/amasotti/qalam/commit/273d91ea18484d749ed7ef87b873dd3ba5ffaab2))

- *(M1)* Setup of ktor framework ([9deadf2](https://github.com/amasotti/qalam/commit/9deadf2141c180ad168b8b1e4801641a9808c2ff))

- *(2.1)* Scaffold frontend ([8e21c85](https://github.com/amasotti/qalam/commit/8e21c851850ea72da18ec71c9208f7bfb358ca27))

- Add favicon ([bf45f87](https://github.com/amasotti/qalam/commit/bf45f87f547c8505642d6cb5443eacbc9c5dcda6))

- Add font support ([50cdec1](https://github.com/amasotti/qalam/commit/50cdec155e3dfafba872d2b9e5b6129bc92bf811))

- Main layout and app.css modularization ([84bce3d](https://github.com/amasotti/qalam/commit/84bce3deac3e061123a9969b4e8cafcb36e06c69))

- Add tanstack query for svelte ([1e5fae0](https://github.com/amasotti/qalam/commit/1e5fae0e0d98a6f5102e311b2a5bd9a87c8a3eda))

- Add local proxy for backend via vite ([e05b878](https://github.com/amasotti/qalam/commit/e05b878bd001016454870dd0443516b9fe314820))

- Main fe layout ([a9eac16](https://github.com/amasotti/qalam/commit/a9eac162ae6199bee8099a6e33fe6ceaf49c91e3))

- Add migration for root table ([b0be86a](https://github.com/amasotti/qalam/commit/b0be86ac6b8cabf0a6424753b74cfbb7c5a874eb))

- Root model and root normalizer ([28cb49a](https://github.com/amasotti/qalam/commit/28cb49a3d0e819be99767ceb937073d9cb497f79))

- Root db layer ([1e1e5d9](https://github.com/amasotti/qalam/commit/1e1e5d9e96cb7603121163ab7a70b70418c41823))

- Implement the root service ([3c424c2](https://github.com/amasotti/qalam/commit/3c424c2efeabcf4bf5848ba76dbc9e32f4e46e1e))

- Implement routing for roots endpoints ([f2c92fc](https://github.com/amasotti/qalam/commit/f2c92fc6517f539f0854ae48a6b9d01a0f224ddb))

- Update CreateRootRequest to use 'root' instead of 'letters' ([c42c4ae](https://github.com/amasotti/qalam/commit/c42c4aef650df263a05c6b862226de5caa17fee6))

- Containerize backend ([d01667f](https://github.com/amasotti/qalam/commit/d01667f874c81dd6e16ecc7b59425fa70c693581))

- Add vocabulary management tables and indexes for words, dictionary links, and progress tracking ([66ed434](https://github.com/amasotti/qalam/commit/66ed4346a5cf275dff50ba3dd59d6a3551957b9b))

- Word entity and related classes ([be97583](https://github.com/amasotti/qalam/commit/be9758357d81f7c9aac9feffda032cfcbb7bd8b8))

- Word routing ([d93904c](https://github.com/amasotti/qalam/commit/d93904c554eee55a658fdc0f43d1b77283bbdc07))

- AI client for word examples ([d158b71](https://github.com/amasotti/qalam/commit/d158b71e8ed75a8501a944a8383c93db7c26284e))

- Add OpenAPI TypeScript configuration and auto-generated client files ([1a621ec](https://github.com/amasotti/qalam/commit/1a621ec573fc529cd3048bd1f3274f8a0b72e41f))

- Design texts endpoints ([8b98530](https://github.com/amasotti/qalam/commit/8b985307b45ee731a4d3ade0202f3c8e07d324dc))

- Implement texts and text_tags tables with initial schema and triggers ([334211e](https://github.com/amasotti/qalam/commit/334211e0ae3ebe0d76c8627920e097663f7d0932))

- Refactor application module setup and add test module for Koin integration ([44078bd](https://github.com/amasotti/qalam/commit/44078bda38f7f43b8981dd7ce83c116f21b54407))

- Add text management module with repository, service, and routes ([835fc19](https://github.com/amasotti/qalam/commit/835fc197cdb9c1e6bda09fb2df4cecbede7e2406))

- Implement domain, infrastructure, and application layers for text management with CRUD operations and routing ([a85ec47](https://github.com/amasotti/qalam/commit/a85ec4772245aab042b2fb4fd639fad1412a796a))

- Api model for sentence endpoints ([2794ae0](https://github.com/amasotti/qalam/commit/2794ae0ddd4a121ccbf84e49f9c8dcae2f12b644))

- Db migrations and repositories for sentences ([acb1647](https://github.com/amasotti/qalam/commit/acb1647e983a02798dddbc0c897c6254fb1d9d8d))

- Db migrations and repositories for sentences ([0c576d6](https://github.com/amasotti/qalam/commit/0c576d61590d03a090441018cbe9790d32db005d))

- Sentence dto and classes ([8914e49](https://github.com/amasotti/qalam/commit/8914e49126b963e28f3d994896fc104041cadee1))

- Implement auto-tokenization and transliteration for Arabic sentences ([9e739f4](https://github.com/amasotti/qalam/commit/9e739f4da710ff068e582cb5d3dcb405e5e974c0))

- Add Kover plugin for code coverage ([56ee34c](https://github.com/amasotti/qalam/commit/56ee34c15c557d65f6b6ad9008345a8ffd5caa94))

- Add endpoints for annotations and translitteration ([1cc3922](https://github.com/amasotti/qalam/commit/1cc3922a8fda136dd0f01b5b72e365b3f91fe7f6))

- *(M7)* Implement annotations domain, persistence, and REST API ([e2a727d](https://github.com/amasotti/qalam/commit/e2a727de3151a211196b15c7229c627da0557f5a))

- *(M10)* Implement transliteration service and REST endpoint ([7967445](https://github.com/amasotti/qalam/commit/7967445ce375e9cc44d18ee55e96194499e526c7))

- Renew banner ([5a2ed1d](https://github.com/amasotti/qalam/commit/5a2ed1d3033afe19f4464e7fb71b44c760ac3bba))

- Foundation of frontend and linting ([f507042](https://github.com/amasotti/qalam/commit/f507042670ff8cfc6bf2274bc61c2f87f932ed8e))

- M14 - root page ([6a16cfd](https://github.com/amasotti/qalam/commit/6a16cfd4dae4232172dfd33b5a9ffd9daaa13d39))

- Support markdown ([3be13db](https://github.com/amasotti/qalam/commit/3be13dbc47c8166535d16b3e1c225f1fc4b6df17))

- Homepage redesign ([0bf96ea](https://github.com/amasotti/qalam/commit/0bf96ea012060320df3f49f6411bc9327e44daaa))

- Homepage redesign v2 ([3326838](https://github.com/amasotti/qalam/commit/3326838aa28410edca5c774e593e61dcf201d967))

- M15 - vocabulary ([ffb23a5](https://github.com/amasotti/qalam/commit/ffb23a5fce38ea28a0d89bf1909680f9ca81eebd))

- Autofill dictionary links ([37a5a99](https://github.com/amasotti/qalam/commit/37a5a99d393ec47310d093ac15e497e77990a7c8))

- Word example setup ([ea8dbf5](https://github.com/amasotti/qalam/commit/ea8dbf5368828116ff36f010faa5acd7cefdc002))

- Word example setup ([b62d562](https://github.com/amasotti/qalam/commit/b62d56256e9a9f1d3e3d97f922bd23f41e378d1f))

- Word example setup (frontend adjustments) ([35b10ce](https://github.com/amasotti/qalam/commit/35b10ce142062f65419eb2ae8a6282f0813aa052))

- Enhance dictionary links UI and functionality ([ac3f810](https://github.com/amasotti/qalam/commit/ac3f8108839c76e4ee65c853c8391314639c737c))

- Implement auto-fill for pronunciation URL and enhance button styles ([58bb125](https://github.com/amasotti/qalam/commit/58bb125c3eff5661e8b71f820c47d1c7075de7b7))

- M16 - text feature ([ca471df](https://github.com/amasotti/qalam/commit/ca471df04dc4cb35570e11c26c3577f6aecc812b))

- Enhance text editing features with collapsible info panel and improved UI ([fbf5efa](https://github.com/amasotti/qalam/commit/fbf5efaeb1c544cf2f8fa2956b1d8a44d1b8051c))

### 🐛 Bug Fixes


- Ai-client injection for tests ([ae9d596](https://github.com/amasotti/qalam/commit/ae9d5969f33107c721a3d5500333a42af82c91cd))

### 🚜 Refactor


- Remove deprecated swagger codegen v3, use generate-types in frontend ([153a521](https://github.com/amasotti/qalam/commit/153a521d5f6fba6e0145c0225937c72fab86bff5))

- Avoid having a separate entry point for tests ([a3df675](https://github.com/amasotti/qalam/commit/a3df67544c5a86608f621a637f6bf564451fd9ef))

- Change const to let for variable declarations in Svelte components ([55e56a7](https://github.com/amasotti/qalam/commit/55e56a79f2d48d3baf6e512aa0364e98e2ee2956))

- General purpose extension for Exposed ([f8adcc1](https://github.com/amasotti/qalam/commit/f8adcc1cf8da45ceae108aeed0ace87489b911e9))

### 🧪 Testing


- Word service ([4e12a5e](https://github.com/amasotti/qalam/commit/4e12a5e92ba4ff300a051d33c86874c3261d9426))

- Complete M5 write tests for text entities ([9f31a9e](https://github.com/amasotti/qalam/commit/9f31a9e35bb331589d9c3ec69d39b2b64ad30156))

- Sentence milestone ([478434f](https://github.com/amasotti/qalam/commit/478434f360f1b378f368c07046b4dde194beaefd))

- Improve setup base integration test ([3198102](https://github.com/amasotti/qalam/commit/3198102e81bf6e64e6b652465ca9e93088729835))

- Fix word tests after refactoring ([934553b](https://github.com/amasotti/qalam/commit/934553b38e130bd03da5f3c22351977e34ec2c2b))

- Post-refactoring this test is not needed anymore ([2f90c9d](https://github.com/amasotti/qalam/commit/2f90c9d1c35ab38e3c2e33cc609dd082cc36cfc9))

### ⚙️ Miscellaneous Tasks


- Rename to qalam ([3880e3a](https://github.com/amasotti/qalam/commit/3880e3a85baeeaffc584bd3486be0987e8202440))

- Add claude settings ([bf18235](https://github.com/amasotti/qalam/commit/bf18235e0c8daac6847e499f6e1663717eddfbc9))

- *(0.4-0.5)* Skeleton setup justfile ([d59d6da](https://github.com/amasotti/qalam/commit/d59d6da1482774383b0547cf2952602f418398c5))

- Add claude.md ([0388fdf](https://github.com/amasotti/qalam/commit/0388fdfd23b32da1a6c712ad7ab2902de2370ee0))

- Agents.md is a symlink to claude ([727823e](https://github.com/amasotti/qalam/commit/727823e626c9f2b40ee3b0c2582d6abbf02e1d12))

- Adjust permissions ([4e8ce21](https://github.com/amasotti/qalam/commit/4e8ce213e8ad2be1bdfcc12ee9977ffbc461f781))

- Update plan ([a2eed42](https://github.com/amasotti/qalam/commit/a2eed420786783503b053d34f8ea8e717077115b))

- Use latest vesions for nix actions ([4ae906c](https://github.com/amasotti/qalam/commit/4ae906cc2da06d2fa59c572796ddd3b5b660ed2c))

- Rename CI ([0aad2c3](https://github.com/amasotti/qalam/commit/0aad2c3b0f587462080732917389044122e1d9fd))

- Remove cache (pay to use) ([758a6f5](https://github.com/amasotti/qalam/commit/758a6f5f22d38822697ba002d0a041e9b57e3b0d))

- Allow ktor wildcard import ([db57181](https://github.com/amasotti/qalam/commit/db571813b6d88baaaac58a453dc1f93e99259897))

- Enable native access for JavaExec tasks ([587a35b](https://github.com/amasotti/qalam/commit/587a35b794347f56737457f7077c02ea601af4bd))

- Add cors whitelist for svelte frontend ([f71e8bc](https://github.com/amasotti/qalam/commit/f71e8bca7513b1c1315497e982b119bff0d2fba2))

- Test fe stack ([9ab1868](https://github.com/amasotti/qalam/commit/9ab186813754a62646c5f346bf4a476253833ba7))

- *(ai)* Improve task management and context budget ([b29a00f](https://github.com/amasotti/qalam/commit/b29a00f2b60344419c2e65facc3ea8b5fd3a7cc5))

- Prepare new task file ([155f215](https://github.com/amasotti/qalam/commit/155f215491413d203ab4a019f6efb2fbad816d91))

- Add note on frontend tooling ([c6ff126](https://github.com/amasotti/qalam/commit/c6ff126a681e8ee817244f13af7e334e88df2469))

- Update kotest and add opt-in for UUID ([e3ed812](https://github.com/amasotti/qalam/commit/e3ed812cbd464ec1db927a96500b55c670f74758))

- Remove nix - overkill for this project and setup ([7008ae3](https://github.com/amasotti/qalam/commit/7008ae3e952e507e0e79e2fa524964041df27749))

- Fix and update action ([320f62f](https://github.com/amasotti/qalam/commit/320f62f411cfbf04c9643599ca5db5cbf7993e2e))

- Update detekt config ([0c8147d](https://github.com/amasotti/qalam/commit/0c8147df9f30bc7e9a38eefaf6326cfb2ec9b624))

- Add DB triggers for updated NOW ([1710746](https://github.com/amasotti/qalam/commit/1710746321917846d99a229c15fc56f7548ed3a9))

- Improve repositories using either blocks ([2280303](https://github.com/amasotti/qalam/commit/2280303a151e1a226b31d90de0d214c942b5546a))

- Update application port to 8085 (conflict with other apps) ([5403ac3](https://github.com/amasotti/qalam/commit/5403ac3f4e4ffc67c7120499d1d989b51d4367a0))

- Relax detekt rules ([f5279b9](https://github.com/amasotti/qalam/commit/f5279b979164a9e57eb7545e11c95ce7d315716b))

- Update .gitignore and documentation.yaml for new directories and API descriptions ([ea08bef](https://github.com/amasotti/qalam/commit/ea08bef83ed2cfc6aea3e4571bb933b850034ccd))

- Remove nonsense requirement ([e82d75c](https://github.com/amasotti/qalam/commit/e82d75caa6a3499e7ceb96cc9cc4f87bdcc24b74))

- Minimal changes in banner ([6ed253a](https://github.com/amasotti/qalam/commit/6ed253aed43f35eb71d5f15b59cdfe027a25aa58))

- Lint ([8d48ca9](https://github.com/amasotti/qalam/commit/8d48ca9c0c2694bce1640cfc5b2dd7cc4b7c2d21))

- Tooling - dependabot and cliff.toml ([30d756d](https://github.com/amasotti/qalam/commit/30d756db7a1b65e43cfc5f5abdf63c63a90b5aae))

- Update deps ([9a6520b](https://github.com/amasotti/qalam/commit/9a6520b67fd6fa7d410b98803b7f32f1b6868819))

- Update deps frontend and lint ([08195e8](https://github.com/amasotti/qalam/commit/08195e871d8e5414b79dbe7e9eba324327db8c18))

- Improve forvo handling ([890d4e0](https://github.com/amasotti/qalam/commit/890d4e070e1bdfb19f02c0b6508d19797f7c6d3b))

- Improve forvo handling ([93f6dfb](https://github.com/amasotti/qalam/commit/93f6dfb01d07b010dcf1ddbeaee642ae8499b603))

- Clean up imports and improve code formatting in multiple components ([ac70838](https://github.com/amasotti/qalam/commit/ac708385364f8366affb316449effaccac32d1a6))

- Add linter overrides for Svelte files to adjust style and correctness rules ([dafd7f5](https://github.com/amasotti/qalam/commit/dafd7f58c402ea1090850e58f1f43ad9c694d779))

- Lint ([0646e7c](https://github.com/amasotti/qalam/commit/0646e7cbf525a08e8fc2672b044b5095d6bf89a4))

<!-- generated by git-cliff -->
