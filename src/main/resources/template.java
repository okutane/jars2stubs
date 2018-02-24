package $package;

public $classKind $className {
    #foreach($field in $fields)
        public $field.type $field.name;
    #end

    #foreach($method in $methods)
        $method.flags $method.returnType $method.name (
                #foreach($parameterType in $method.parameterTypes)
                    $parameterType arg$foreach.index #if($foreach.hasNext ),#end
                #end
        );
    #end
}
