### Пример демонстрирующий возможность работы библиотеки ПЖ с SQL-оператором RETURNING.
#### Требования к конфигурации
1. При настройке бина *proxyDataSource* в метод билдера *queryTransformer*
   вместо *ProxyQueryTransformer* требуется передать *SaveReturningProxyQueryTransformer*.
2. При настройке бина *visitFactory* использовать конструкцию:
```java
public IVisitorFactory<Change> visitFactory() {
   return new VisitorFactory(
           new RewindCrawlerFactory()
   );
}
```
#### Ограничения:
Cписок полей для RETRURNING будет трансформирован, и будут добавлены фактически все поля записи то могут не работать методы SpringData ожидающие что возвращаемый ResultSet будет содержать только одну колонку. Могут генерироваться исключения вида

```java 
Exception in thread "main" org.springframework.jdbc.IncorrectResultSetColumnCountException: Incorrect column count: expected 1, actual 5
```

В качестве обходного решения можно использовать мапперы, которые обрабатывают ResultSet явно, такие как:

org.springframework.jdbc.core.RowMapper
org.springframework.jdbc.core.ResultSetExtractor