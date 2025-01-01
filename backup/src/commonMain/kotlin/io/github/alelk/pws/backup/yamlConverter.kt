package io.github.alelk.pws.backup

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

internal val yamlConverter = Yaml(configuration = YamlConfiguration(multiLineStringStyle = MultiLineStringStyle.Literal))