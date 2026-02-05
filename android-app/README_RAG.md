# Faz 3: RAG Pipeline + Chat'e Bağlama

## Amaç

Soru → embedding → top-k chunk → LLM'e context ile prompt → cevap; bunu Chat ekranında göstermek.

**Başarı kriteri:** Chat'te soru sorulunca, PDF'lerden alınan chunk'lara dayalı gerçek model cevabı görünüyor; isteğe bağlı "kaynak: sayfa X, Y" bilgisi eklenebiliyor.

---

## Ön koşul: Faz 1 + Faz 2

- **Faz 1:** `LlamaEngine` (JNI), `loadModel(path)`, `prompt(text)` çalışıyor; model yüklü (örn. `filesDir` altında `model.gguf`).
- **Faz 2:** `VectorStoreRepository.searchSimilar(queryText, topK)`, `EmbeddingEngine.embed(text)`, `ChunkEntity` (Room), PDF → chunk → embedding akışı hazır.

Bu README, Faz 3 için yapılacakları adım adım listeler.

---

## 1. RAG akışı (soru → context → LLM → cevap)

### 1.1 Akış özeti

1. Kullanıcı soruyu yazar → `query`.
2. `query` için embedding üret: `embeddingEngine.embed(query)`.
3. Vector store'dan top-k chunk al: `vectorStoreRepository.searchSimilar(query, topK = 5)` (veya `searchSimilar(embedding, 5)`).
4. Prompt şablonu ile context oluştur:  
   `"Şu metne göre cevapla:\n\n{chunks}\n\nSoru: {query}\n\nCevap:"`
5. Context uzunluğu sınırı: Toplam karakter (veya token) limiti koy; gerekirse chunk metinlerini kısalt veya en alakalı olanlarla sınırla.
6. Bu prompt'u `LlamaEngine.prompt(prompt)` ile gönder → cevap metnini al.

### 1.2 Yapılacaklar (kod tarafı)

| Adım | Ne yapılacak |
|------|----------------|
| 1.2.1 | **RAG servis sınıfı:** `RagService` (veya `RagPipeline`) oluştur. Girdi: `query: String`. Çıktı: `Pair<cevapMetni, kaynakChunkListesi>`. İçeride: `VectorStoreRepository`, `EmbeddingEngine`, `LlamaEngine` kullan. |
| 1.2.2 | **Prompt şablonu:** Sabit bir template (örn. `RAG_PROMPT_TEMPLATE`) ile `{chunks}` ve `{query}` yerlerini doldur. Chunk'ları birleştirirken ayırıcı koy (örn. `"\n---\n"`). |
| 1.2.3 | **Context sınırı:** Toplam karakter limiti (örn. 2000–4000) belirle. Chunk'ları sırayla ekle; limiti aşınca kes. İsteğe bağlı: token sayısına göre kes (llama.cpp tokenizer kullanılabilir ama basit versiyonda karakter limiti yeterli). |
| 1.2.4 | **Kaynak bilgisi:** Her chunk'tan `documentId`, `pageNumber` (ve gerekirse `chunkIndex`) al; cevap ile birlikte döndür ki UI'da "Kaynak: sayfa 3, 5" gibi gösterilebilsin. |

### 1.3 Örnek prompt şablonu

```text
Şu metne göre cevapla. Sadece verilen metinde yazanlara dayanarak kısa ve net cevap ver.

{chunks}

Soru: {query}

Cevap:
```

`{chunks}` = top-k chunk'ların `text` alanlarının birleşimi (ayraçla).  
`{query}` = kullanıcının yazdığı soru.

---

## 2. ChatViewModel'e bağlama

### 2.1 Mevcut durum (Tamamlandı)

- `ChatViewModel.sendMessage()`: Kullanıcı mesajını alıyor; RAG pipeline çağrılıyor, cevap + kaynak gösteriliyor.
- Placeholder kaldırıldı; gerçek cevap ve "Kaynak: sayfa …" eklendi.

### 2.2 Yapılacaklar (Tamamlandı)

| Adım | Ne yapılacak |
|------|----------------|
| 2.2.1 | **ChatViewModel'e RAG + LLM bağımlılığı:** `VectorStoreRepository` ve `LlamaEngine` erişimi. `VectorStoreRepository` için: `AppDatabase.getInstance(application).chunkDao()` + `EmbeddingEngine(application)` ile repository oluştur (DocumentsViewModel'deki gibi). |
| 2.2.2 | **sendMessage() içinde RAG pipeline çağrısı:** `viewModelScope.launch(Dispatchers.IO)` ile `RagService` (veya doğrudan pipeline adımlarını) çağır. Girdi: `query = text` (kullanıcı mesajı). Çıktı: cevap metni + isteğe bağlı kaynak listesi (sayfa no). |
| 2.2.3 | **Cevabı UI state'e yaz:** Placeholder yerine gerçek cevabı `ChatMessage(role = "assistant", text = cevapMetni)` olarak ekle. İsteğe bağlı: `text` içine veya ayrı bir alana "Kaynak: sayfa 2, 4" ekle. |
| 2.2.4 | **Hata yönetimi:** Model yüklü değilse veya RAG/LLM hata verirse kullanıcıya anlamlı mesaj göster (örn. "Model yüklenmedi" / "Cevap oluşturulamadı"). |

### 2.3 Model yüklü mü kontrolü (Tamamlandı – Faz 3.2)

- `LlamaEngine.isModelLoaded()` eklendi: `loadModel` / `unload` sonrası durum takip ediliyor.
- `RagService.ask()` başında model yüklü değilse `LlamaEngine.prompt()` çağrılmıyor; kullanıcıya Türkçe mesaj dönülüyor.

---

## 3. Context window (sliding window)

### 3.1 Basit versiyon (önerilen ilk adım)

- Her mesajda **sadece son soru + RAG chunk'ları** kullan.
- Yani conversation history'yi prompt'a ekleme; sadece:  
  `[RAG context] + Soru: {sonSoru} + Cevap:`
- Bu, Faz 3 çıktısı için yeterli ve implementasyonu kolay.

### 3.2 İsteğe bağlı: Son N mesajı context'e ekleme

| Adım | Ne yapılacak |
|------|----------------|
| 3.2.1 | **ChatUiState / ChatViewModel:** Son N mesajı (örn. 5) tut. `messages` zaten listeleniyor; prompt oluştururken son N tanesini al. |
| 3.2.2 | **Prompt şablonu:** "Önceki konuşma: ... Son soru: ..." gibi bir formatla son N mesajı ekle. Toplam token/karakter limitine dikkat et. |
| 3.2.3 | **İleride:** Eski mesajları "archive" edip sadece son birkaç turu context'te tutmak (daha gelişmiş konuşma yönetimi). |

---

## 4. Dosya / sınıf eşlemesi (referans)

| Bileşen | Mevcut konum / not |
|---------|---------------------|
| Chat ekranı | `app/.../ui/screens/ChatScreen.kt` |
| Chat state & sendMessage | `app/.../ui/screens/ChatViewModel.kt` |
| Mesaj modeli | `ChatMessage(id, role, text)` |
| Vector store arama | `VectorStoreRepository.searchSimilar(queryText, topK)` |
| Embedding | `EmbeddingEngine.embed(text)` |
| LLM | `LlamaEngine.prompt(prompt)` |
| Chunk bilgisi | `ChunkEntity`: `documentId`, `pageNumber`, `chunkIndex`, `text` |
| DB erişimi | `AppDatabase.getInstance(context).chunkDao()` |

---

## 5. Sıra özeti (checklist)

| # | Görev | Açıklama |
|---|--------|----------|
| 1 | RAG pipeline sınıfı | `RagService` / `RagPipeline`: query → embed → searchSimilar → prompt template → LlamaEngine.prompt → (cevap, kaynaklar). |
| 2 | Context limiti | Chunk birleştirirken karakter (veya token) limiti; gerekirse chunk'ları kısalt. |
| 3 | ChatViewModel.sendMessage | Placeholder kaldır; RAG pipeline'ı çağır; cevabı + isteğe bağlı kaynakları UI'a yaz. |
| 4 | Hata ve model kontrolü | Model yoksa / hata olursa kullanıcıya anlamlı mesaj. |
| 5 | (Opsiyonel) Son N mesaj | Prompt'a son N mesajı ekle; context window. |

---

## 6. Çıktı

- Chat'te soru sorulunca:
  - Soru metni → embedding → vector store'dan top-k chunk alınır.
  - Chunk'lar prompt şablonunda context olarak kullanılır.
  - LLM cevap üretir; cevap Chat ekranında gösterilir.
  - İsteğe bağlı: "Kaynak: sayfa 2, 4" gibi bilgi gösterilir.

Bu adımlar tamamlandığında Faz 3 hedefine ulaşılmış olur.

---

## 7. Nasıl test edebilirim?

### 7.1 Ön koşullar

| Gereksinim | Açıklama |
|------------|----------|
| **GGUF model** | Telefonda/emülatörde bir GGUF dosyası (örn. Phi-2 veya TinyLlama Q4_K_M). Uygulama açılışında dosya varsa otomatik yüklenir. |
| **Model konumu** | Sırayla aranır: (1) uygulama dizini `.../files/model.gguf`, (2) **sdcard/Download** içinde `model.gguf` veya herhangi bir `.gguf` dosyası. |
| **En az bir PDF işlenmiş** | Belgeler ekranından bir PDF ekleyip **İşle** ile chunk + embedding üretilmiş olmalı. |
| **Embedding modeli** | `embedding_model.tflite` assets’te ise anlamlı chunk araması yapılır; yoksa tüm chunk’lar aynı (sıfır) vektörle eşleşir. |

### 7.2 Adımlar

1. **Modeli cihaza koy (bir kez)**  
   - **Seçenek A:** GGUF dosyasını **sdcard/Download** klasörüne koy (telefonda indirdiysen zaten orada olabilir). Uygulama açılışta `Download` içindeki `model.gguf` veya herhangi bir `.gguf` dosyasını kullanır.  
   - **Seçenek B:** Bilgisayardan push ile uygulama dizinine koy:
   ```bash
   adb push ~/Downloads/phi-2.Q4_K_M.gguf /storage/emulated/0/Android/data/com.example.pocketscholar/files/model.gguf
   ```
   Emülatör kullanıyorsan `~/Downloads/...` yerine kendi indirme yolunu yaz.

2. **Uygulamayı çalıştır**  
   Android Studio’dan Run (veya `./gradlew :app:installDebug` + uygulamayı aç). İlk açılışta model dosyası varsa arka planda yüklenir (birkaç saniye sürebilir).

3. **PDF + chunk hazırla**  
   - **Belgeler** sekmesine git → PDF ekle (veya zaten ekliyse atla).  
   - Eklediğin PDF için **İşle**’ye bas → chunk’lar ve embedding’ler üretilsin.  
   - (Embedding modeli yoksa chunk’lar sıfır vektörle kaydedilir; RAG yine çalışır ama “en alakalı” sıralama anlamlı olmaz.)

4. **Chat’te dene**  
   - **Sohbet** sekmesine geç.  
   - PDF’teki konuyla ilgili bir soru yaz (örn. “Bu metinde ana konu ne?”).  
   - **Gönder**’e bas.  
   - Bir süre “düşünüyor” göstergesi görünür; ardından model cevabı ve (varsa) “Kaynak: sayfa X, Y” satırı gelir.

### 7.3 Ne beklenir?

| Durum | Sonuç |
|-------|--------|
| Model yüklü + chunk var + soru gönderildi | Cevap metni + isteğe bağlı “Kaynak: sayfa …” |
| Model yüklü değil | Cevap alanında “[Model not loaded. Call loadModel() first.]” veya benzeri mesaj |
| Hiç chunk yok (PDF işlenmemiş) | Context “(Verilen metin yok.)” olur; model yine de genel bir cevap üretebilir |
| Embedding modeli yok | Chunk’lar rastgele/sıfır benzerlikle seçilir; cevap yine gelir ama içerik bazlı sıralama olmaz |

### 7.4 Hata ayıklama (Logcat)

- **Etiketler:** `LlamaEngine`, `RagService`, `EmbeddingEngine`, `VectorStoreRepository`  
- Model yükleme: `loadModel` sonrası JNI/log mesajları.
- **Embedding / vocab:** Logcat'te "Embedding norm ≈ 0" görürsen `vocab.txt` ile `embedding_model.tflite` aynı modelden olmalı; uyumsuzsa token ID'ler yanlış olur, vektörler benzer/sıfır çıkar ve yanlış chunk'lar seçilir.
- RAG: `RagService.ask` içinde exception olursa Chat’te “Cevap oluşturulamadı: …” çıkar; Logcat’te stack trace’e bak.

### 7.5 Kısa test (model yoksa)

Model push etmeden de akışı denemek istersen: Chat’te bir şey yazıp Gönder’e bas. Model yüklü değilse cevap olarak “[Model not loaded. Call loadModel() first.]” görürsün; RAG (embedding + chunk arama + prompt oluşturma) yine çalışır, sadece LLM cevabı bu mesaj olur.

### 7.6 Farklı TFLite embedding modeli

EmbeddingEngine farklı TFLite modellerini otomatik algılıyor: **Giriş:** INT32 ise vocab.txt + WordPiece; STRING ise metin doğrudan. **Giriş sayısı:** 1, 2 (input_ids + attention_mask) veya 3. **Çıktı:** [1, dim] veya [1, seq, dim] (3D ise mean-pool). Logcat'te EmbeddingEngine loglarında giriş/çıkış şekillerini görebilirsin. INT32 modelde vocab.txt, embedding_model.tflite ile aynı modelden olmalı.
